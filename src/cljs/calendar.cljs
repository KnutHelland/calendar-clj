(ns kh.calendar
  (:require-macros [hiccups.core :as hiccups])
  (:require [hiccups.runtime :as hiccupsrt]
            [jayq.core :as jayq]))

;; -----------------------------------------------------------------------------
;; Date functions
;;
;; Formats:
;; - unixtime        UTC unix timestamp (seconds since 1970)
;; - monthstamp      UTC unix timestamp, first second in a month
;; - datestamp       UTC unix timestamp, first second in a day
;; - isotime         ISO 8601
;; - datestring      UTC yyyy-mm-dd
;; - datetimestring  UTC yyyy-mm-ddThh:ssz (ISO 8601)
;;

(defn js->unixtime
  "Takes a javascript Date object and returns unixtime"
  [js-date]
  (int (/ (.getTime js-date) 1000)))

(defn unixtime-now
  "Returns current time in unixtime."
  []
  (js->unixtime (js/Date.)))

(defn unixtime->js
  "Creates a Javascript Date object with given unixtime."
  [unixtime]
  (js/Date. (* 1000 unixtime)))

(defn unixtime->monthstamp
  "Gets the monthstamp from the unixtime."
  [unixtime]
  (let [date (unixtime->js unixtime)
        date (doto date
               (.setHours 0 0 0 0)
               (.setDate 1))]
    (js->unixtime date)))

(defn unixtime->datestamp
  "Gets the datestamp from the unixtime"
  [unixtime]
  (let [date (unixtime->js unixtime)
        date (doto date
               (.setHours 0 0 0 0))]
    (js->unixtime date)))

(defn int->str
  [number length]
  (str (apply str (repeat (- length (count (str number))) "0")) number))

(defn unixtime->datestring
  "Returns a datestring from a unixtime"
  [unixtime]
  (let [date (unixtime->js unixtime)
        year (int->str (.getUTCFullYear date) 2)
        month (int->str (+ 1 (.getUTCMonth date)) 2)
        date (int->str (+ 1 (.getUTCDate date)) 2)]
    (str year "-" month "-" date)))

(defn unixtime->datetimestring
  "Returns a datetimestring (UTC ISO 8601) from a unixtime"
  [unixtime]
  (let [date (unixtime->js unixtime)
        hour (int->str (.getUTCHours date) 2)
        minute (int->str (.getUTCMinutes date) 2)
        second (int->str (.getUTCSeconds date) 2)]
    (str (unixtime->datestring unixtime) "T"
         hour ":" minute ":" second "Z")))

#_(defn unixtime->datetimestring
  "Returns a datetimestring from a unixtime"
  [unixtime]
  (.toISOString (unixtime->js unixtime)))

(defn get-weeknumber
  "Returns the weeknumber of a timestamp.
  Conversion as from
  http://stackoverflow.com/questions/6117814/get-week-of-year-in-javascript-like-in-php"
  [unixtime]
  (let [js-date (unixtime->js unixtime)
        day-number (if (= (.getDay js-date) 0) 7 (.getDay js-date))
        js-date (doto js-date
                  (.setHours 0 0 0)
                  (.setDate (- (+ 4 (.getDate js-date)) day-number)))
        year-start (js/Date. (.getFullYear js-date) 0 1)]
    (.ceil js/Math (/ (+ (/ (- js-date year-start) 86400000) 1) 7))))

(defn datestamps-from
  "Creates a lazy seq with datestamps including given datestamp"
  [unixtime]
  (let [datestamp (unixtime->datestamp unixtime)]
    (cons datestamp (lazy-seq (datestamps-from (+ (* 26 3600) datestamp))))))

(defn monthstamps-from
  "Creates a lazy seq with monthstamps including given montstamp"
  [unixtime]
  (let [monthstamp (unixtime->monthstamp unixtime)]
    (cons monthstamp (lazy-seq (monthstamps-from (+ (* 35 26 3600) monthstamp))))))

(defn weeknumbers-from
  "Creates a lazy sequence with weeknumbers starting from unixtime"
  [unixtime]
  (let [datestamp (unixtime->datestamp unixtime)
        weeknumber (get-weeknumber datestamp)]
    (cons weeknumber (lazy-seq (weeknumbers-from (+ (* 7 25 3601) datestamp))))))

#_(defn datetimestring->unixtime
  "Parses the RFC 3339 formatted string:"
  [str]
  (let [all
        (re-find #"(?i)^(\d{4})\-(\d{2})\-(\d{2})(|T(\d{2}):(\d{2})\:(\d{2})(Z|(\+|\-)(\d{2})\:(\d{2})))$" str)
        [_ y m d time h min s offset sign offsethours offsetminutes]
        (re-find #"(?i)^(\d{4})\-(\d{2})\-(\d{2})(|T(\d{2}):(\d{2})\:(\d{2})(Z|(\+|\-)(\d{2})\:(\d{2})))$" str)]
    (.log js/console (pr-str all))
    (if (clojure.string/blank? time)
      (js->unixtime (js/Date. y (- m 1) d 0 0 0))
      (let [as-client-timezone (js->unixtime (js/Date. y (- m 1) d h min s))
            client-timezoneoffset (.getTimezoneOffset (js/Date.))
            as-utc-timezone (+ as-client-timezone (* client-timezoneoffset 60000))]
        (if (= (clojure.string/upper-case offset) "Z")
          as-utc-timezone
          (if (= sign "+")
            (+ (* offsethours 36000000) (* offsetminutes 60000) as-utc-timezone)
            (- (* offsethours 36000000) (* offsetminutes 60000) as-utc-timezone)))))))

(defn datetimestring->unixtime
  "Parses the RFC 3339 formatted string:"
  [str]
  (int (/ (.parse js/Date str) 1000)))

;; -----------------------------------------------------------------------------
;; Render functions
;; 

(defn- get-first-datestamp-in-calendar
  "Returns the datestamp of the first monday in the calendar with given
  monthstamp"
  [monthstamp]
  (let [monthstamp (unixtime->monthstamp monthstamp)
        first-day-of-month (.getDay (unixtime->js monthstamp))
        first-day-of-month (if (= 0 first-day-of-month) 7 first-day-of-month)
        first-date (unixtime->js (- monthstamp (* 3600 24 (- first-day-of-month 1))))]
    (js->unixtime first-date)))

(def list-of-months
  ["Januar" "Februar" "Mars" "April"
   "Mai" "Juni" "Juli" "August"
   "September" "Oktober" "November" "Desember"])

(def list-of-short-days
  ["Man" "Tir" "Ons" "Tor" "Fre" "Lør" "Søn"])

(defn- get-table-header
  [monthstamp]
  (let [month (unixtime->js monthstamp)]
    (str (get list-of-months (.getMonth month)) " " (.getFullYear month))))

(defn month->html
  "Returns a month as html. Timestamp is a timestamp within the month
  to be displayed. event-model contains all events."
  [unixtime]
  (let [monthstamp (unixtime->monthstamp unixtime)
        today (unixtime->datestamp (unixtime-now))
        weeknumbers (weeknumbers-from monthstamp)
        first-datestamp (get-first-datestamp-in-calendar monthstamp)
        datestamps (take 42 (datestamps-from first-datestamp))]
    (hiccups/html
     [:table {:class "calendar" :data-month monthstamp}
      [:tr [:th {:colspan "8" :class "monthname"}
            (get-table-header monthstamp)]]
      [:tr
       [:th " &nbsp; "]
       #_(.log js/console (filter #([:th %]) list-of-short-days))
       [:th {:class "dayname"} " Man "]
       [:th {:class "dayname"} " Tir "]
       [:th {:class "dayname"} " Ons "]
       [:th {:class "dayname"} " Tor "]
       [:th {:class "dayname"} " Fre "]
       [:th {:class "dayname"} " Lør "]
       [:th {:class "dayname holiday"} " Søn "]]
      (for [row (range 6)
            :let [weeknumber (nth weeknumbers row)
                  printed-days (* row 7)]]
        [:tr
         [:td {:class "weeknumber"} weeknumber]
         (for [datestamp (take 7 (drop printed-days datestamps))
               :let [sunday? (= 0 (.getDay (unixtime->js datestamp)))
                     outsider? (or (< datestamp monthstamp)
                                   (>= datestamp (second (monthstamps-from monthstamp))))
                     passed? (< datestamp today)
                     today? (= datestamp today)]]
           [:td
            {:class (str "date" (if sunday? " holiday") (if today? " today")
                         (if outsider? " outsider") (if passed? " passed"))
             :data-datestamp datestamp}
            (.getDate (unixtime->js datestamp))])])])))



;; -----------------------------------------------------------------------------
;; Manipulating the calendar
;;

(defn get-date-cells
  "Returns jQuery object with all cells that represents the given date."
  [context datestamp]
  (let [datestamp (unixtime->datestamp datestamp)]
    (jayq/$ (str ".date[data-datestamp=\"" datestamp "\"]:not(.outsider)") context)))

(defn- register-event
  ([event context callback]
     "Register a callback on all dates"
     (jayq/on (jayq/$ ".date" context) event callback))
  ([event context callback datestamp]
     "Register a callback on given date"
     (jayq/on (get-date-cells context datestamp) event callback)))

(def register-onclick
  (partial register-event "click"))

(def register-onhold
  (partial register-event "hold"))

(defn create-canvas
  "Creates a empty canvas with size 100x100"
  []
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-height canvas) 100)
    (set! (.-width canvas) 100)
    canvas))

(defn- color-canvas
  "Gives colors to a canvas and returns it."
  [canvas colors]
  (let [w (.-width canvas)
        h (.-height canvas)
        c (.getContext canvas "2d")]
    (if (= 2 (count colors))
      (do
        (set! (.-fillStyle c) (first colors))
        (doto c
          (.beginPath)
          (.moveTo 0 0)
          (.lineTo w 0)
          (.lineTo 0 h)
          (.closePath)
          (.fill))
        (set! (.-fillStyle c) (second colors))
        (doto c
          (.beginPath)
          (.moveTo 0 h)
          (.lineTo w 0)
          (.lineTo w h)
          (.closePath)
          (.fill)))
      (doall
       (for [i (range (count colors)) :let [color (nth colors i)]]
         (do
           (set! (.-fillStyle c) color)
           (.fillRect c 0 (* (/ h (count colors)) i) w (/ h (count colors)))))))
    canvas))

;; #_(defn- circle-canvas
;;   "Gives colors to a canvas and returns it."
;;   [canvas color]
;;   (let [w (.-width canvas)
;;         h (.-height canvas)
;;         c (.getContext canvas "2d")]
;;     (do
;;       (set! (.-strokeStyle c) (first color))
;;       (set! (.-lineWidth c) 5)
;;       (doto c
;;         (.beginPath)
;;         (.arc (/ w 2) (/ h 2) (- (/ w 2) 3) 0 (* (.-PI js/Math) 2) false)
;;         (.stroke)))
;;     canvas))

(defn set-background-colors
  ([context datestamp]
     "Removes the background color from the date."
     (jayq/css (get-date-cells context datestamp) :background "none"))

  ([context datestamp colors]
     "Sets background colors to a date. Colors can be
     eighter a color or a sequence of colors. If colors is a
     empty list, the background color is removed."
     (let [colors (if (string? colors) (list colors) colors)]
       (if (empty? colors)
         (set-background-colors context datestamp)
         (let [canvas (color-canvas (create-canvas) colors)
               bg (str "url(" (.toDataURL canvas "image/png") ")")]
           (doall
            (for [day (get-date-cells context datestamp)]
              (do
                (jayq/attr (jayq/$ day) :data-colors (apply str (map #(str % " ") colors)))
                (jayq/css (jayq/$ day) {:background-image bg :background-size "100% 100%"})))))))))

(defn add-background-color
  "Adds a background color to a date."
  [context datestamp colors]
  (let [colors (if (string? colors) (list colors) colors)]
    (doall
     (for [day (get-date-cells context datestamp)
           :let [old (clojure.string/split (jayq/attr (jayq/$ day) :data-colors) #"\s")
                 new-colors (remove empty? (concat old colors))]]
       (set-background-colors (.-parentNode day) datestamp new-colors)))))

(defn add-class
  "Adds a class to a date."
  [context datestamp class]
  (doall
   (for [day (get-date-cells context datestamp)]
     (jayq/add-class (jayq/$ day) class))))

(defn set-text-color
  ([context datestamp]
     "Removes the text color on a date."
     (jayq/css (get-date-cells context datestamp) :color "inherit"))
  ([context datestamp color]
     "Sets the text color on a date."
     (jayq/css (get-date-cells context datestamp) :color color)))


;; -----------------------------------------------------------------------------
;; Datamodel and AJAX
;;

#_(def example-data [{:title "Eksamen"
  :start "2013-06-03T00:00z"
  :end "2013-06-03T23:59z"
  :color "#432343"},
 {:title "Ut på tur"
  :start "2013-06-03T00:00z"
  :end "2013-06-06T23:59z"
  :color "#328954"},
 {:title "St. hans"
  :start "2013-06-23T19:30z"
  :end "2013-06-23T23:59z"
  :color "#432343"},
 {:title "Ingenting"
  :start "2013-06-19T00:00z"
  :end "2013-06-19T23:59z"
  :color "#333333"}])

(defn load-data
  [context data]
  (dorun
   (for [{:keys [title start end color class]} data]
     (dorun
      (for [datetime (datestamps-from (datetimestring->unixtime start))
            :while (< datetime (unixtime->datestamp (datetimestring->unixtime end)))]
        (do
          (if (not (clojure.string/blank? color))
            (add-background-color context datetime color))
          (if (not (clojure.string/blank? class))
            (add-class context datetime class))))))))

(defn load-events-from-url
  "Loads events from a url."
  [url callback]
  (jayq/ajax
   url
   {:data-type "text/clojure"
    :success callback}))


;; -----------------------------------------------------------------------------
;; Initialization
;;

(def data (atom []))

(defn list-events-on-date
  [datestamp]
  (apply str
         (map #(if (not (empty? %)) (str % "\n"))
              (for [{:keys [title start end]} @data]
                (if (and (>= datestamp
                             (unixtime->datestamp (datetimestring->unixtime start)))
                         (< datestamp
                            (unixtime->datestamp (unixtime->datestamp (datetimestring->unixtime end)))))
                  title)))))

(defn click
  [event]
  (let [datestamp (jayq/attr (jayq/$ (.-currentTarget event)) "data-datestamp")
        events (list-events-on-date datestamp)]
    (if (not (clojure.string/blank? events))
      (.alert js/window events))))

(defn ^:export init
  "Displays next months in given element."
  [selector]
  (let [elm (.querySelector js/document selector)]
    (do (set!
         (.-innerHTML elm)
         (apply str
                (for [monthstamp (take 12 (monthstamps-from (unixtime-now)))]
                  (month->html monthstamp))))
        (load-events-from-url
         "server.php"
         (fn [d]
           (do
             (reset! data d)
             (load-data js/document @data))))
        (register-onclick js/document click))))
