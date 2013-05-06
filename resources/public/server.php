<?php
/** ****************************************************************************
 *  Google Calendar Client
 */

error_reporting(E_ALL);


/*
 * Cache
 */
define('CACHE', 'cache.dat');
if (file_exists(CACHE)) {
    if (filemtime(CACHE) > (time()-60)) {
        header("Content-type: text/clojure");
        echo file_get_contents(CACHE);
        exit();
    }
}

/*
 * Load settings
 */
define('SETTINGS_FILE', '.settings.php');
define('TOKEN_FILE', '.token.php');
define('SCOPE', 'https://www.googleapis.com/auth/calendar');
define('GCAL', 'https://www.googleapis.com/calendar/v3');

if (file_exists(SETTINGS_FILE)) {
    require SETTINGS_FILE;
} else {
    echo 'Need to create a '.SETTINGS_FILE.' file with the following definitions:';
    echo 'CLIENT_ID, CLIENT_SECRET, REDIRECT';
}

/*
 * CUrl
 */

function curl_simple($url, $method = "get", $fields = array()) {
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    if (strtolower($method) == 'post') {
        $fields_string = '';
        foreach ($fields as $key => $value)
            $fields_string .= $key.'='.urlencode($value).'&';
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $fields_string);
    }
    $response = curl_exec($ch);
    curl_close($ch);
    return $response;
}    

/*
 * OAuth
 */

/** Reads the request to see if we got a new api code */
function oauth_read_request() {
    if (isset($_GET['error']) && $_GET['error'] == 'access_denied') {
        oauth_redirect_to_login();
        exit();
    }
    if (isset($_GET['code'])) {
        oauth_get_access_token($_GET['code']);
        header("location: server.php");
        exit();
    }
}

/** Get auth token */
function oauth_get_access_token($code) {
    $params = array(
        'code' => $code,
        'client_id' => CLIENT_ID,
        'client_secret' => CLIENT_SECRET,
        'redirect_uri' => REDIRECT,
        'grant_type' => 'authorization_code'
    );
    $result = json_decode(curl_simple(
        'https://accounts.google.com/o/oauth2/token', 'post', $params));
    if (isset($result->error)) {
        header('location: server.php');
        exit();
    }

    define('TOKEN', $result->access_token);
    define('REFRESH', $result->refresh_token);

    $fp = fopen(TOKEN_FILE, 'w');
    fwrite($fp, '<?php define("TOKEN", "'.TOKEN.'"); define("REFRESH", "'.REFRESH.'");');
    fclose($fp);
    chmod(TOKEN_FILE, 0777);
}

function oauth_refresh_token() {
    if (!defined('REFRESH')) {
        oauth_redirect_to_login();
        exit();
    }

    $params = array(
        'refresh_token' => REFRESH,
        'client_id' => CLIENT_ID,
        'client_secret' => CLIENT_SECRET,
        'grant_type' => 'refresh_token'
    );
    $result = json_decode(curl_simple(
        'https://accounts.google.com/o/oauth2/token', 'post', $params));
    if (isset($result->error)) {
        header('location: server.php');
        exit();
    }

    
    $fp = fopen(TOKEN_FILE, 'w');
    fwrite($fp, '<?php define("TOKEN", "'.$result->access_token.'");'.
                'define("REFRESH", "'.REFRESH.'");');
    fclose($fp);
    chmod(TOKEN_FILE, 0777);
    header('location: server.php');
    exit();
}


/** Loads saved access token from .token.php */
function oauth_load_saved_access_token() {
    if (file_exists('.token.php')) {
        require '.token.php';

        if (oauth_validate_access_token())
            return;
        else
            oauth_refresh_token();
             
    }

    oauth_read_request();
    if (!defined('TOKEN') || !oauth_validate_access_token())
        oauth_redirect_to_login();
}

/** Validates access token. TODO: Bad solution. */
function oauth_validate_access_token() {
    $calendars = gcal_get_calendars();
    return isset($calendars->items) && count($calendars->items) > 0;
}

/** Redirects user to Google login page. */
function oauth_redirect_to_login() {
    header('location: https://accounts.google.com/o/oauth2/auth?'.
           'client_id='.CLIENT_ID.'&redirect_uri='.REDIRECT.
           '&access_type=offline&approval_prompt=force'.
           '&scope='.SCOPE.'&response_type=code');
    exit();
}


/*
 * Clojure
 */

function is_assoc($array) {
    return (array_keys($array) !== range(0, count($array) - 1));
}

function clojure_encode($input) {
    if (is_integer($input)) {
        return ' '.$input.' ';
    }

    if (is_array($input)) {
        if (!is_assoc($input)) {
            $buffer = "[";
            foreach ($input as $value) {
                $buffer .= clojure_encode($value) . " ";
            }
            $buffer = trim($buffer);
            return trim($buffer) . "]";
        } else {
            $buffer = "{";
            foreach ($input as $key => $value) {
                if (is_string($key))
                    $key = ':'.$key;
                else
                    $key = clojure_encode($key);
                $buffer .= $key.' '.clojure_encode($value).' ';
            }
            $buffer = trim($buffer);
            return $buffer .= "}";
        }
    }

    if (is_object($input)) {
        return clojure_encode(get_object_vars($input));
    }

    return ' "'.$input.'" ';
}        

/*
 * Google Calendar
 */

function gcal_get($url, $params = array()) {
    $params_string = "";
    foreach ($params as $key => $value) {
        $params_string .= "&".$key."=".urlencode($value);
    }
    return json_decode(curl_simple(
        GCAL.$url.'?alt=json&oauth_token='.TOKEN.$params_string));
}

function gcal_get_calendars() {
    return gcal_get('/users/me/calendarList');
}

function gcal_get_events($calendar_id) {
    return gcal_get('/calendars/'.$calendar_id.'/events',
                    array('singleEvents' => 'True',
                          'timeMin' => date('c', strtotime("-1 month"))));
}


/*
 * Implementation
 */
function ifsetor(&$var, $alt) {
    if (isset($var) && !empty($var)) {
        return $var;
    }
    return $alt;
}

function googleColors($num) {
    $googleColors = array(
        '1' => '#a4bdfc',
        '2' => '#7ae7bf',
        '3' => '#dbadff',
        '4' => '#ff887c',
        '5' => '#fbd75b',
        '6' => '#ffb878',
        '7' => '#46d6db',
        '8' => '#e1e1e1',
        '9' => '#5484ed',
        '10' => '#51b749',
        '11' => '#dc2127'
    );
    return $googleColors[$num];
}


function get_norwegian_holidays() {
    $from = strtotime('-1 year');
    $to = strtotime('+2 years');
    $fromYear = mktime(0, 0, 0, 1, 1, date("Y", $from));
    $toYear = mktime(0, 0, 0, 13, 1, date("Y", $to));

    $year = $fromYear;
    $redDates = array();
    $holidays = array();
    while ($year <= $toYear) {
        $easter = easter_date(date("Y", $year));
        
        $holidays[] = array('Påskedag',
                            $easter);
        $holidays[] = array('Skjærtorsdag',
                            strtotime('-3 days', $easter));
        $holidays[] = array('Langfredag',
                            strtotime('-2 days', $easter));
        $holidays[] = array('Andre påskedag',
                            strtotime('+1 day', $easter));
        $holidays[] = array('Nyttårsdag',
                            strtotime('1th january', $year));
        $holidays[] = array('Arbeidernes fridag',
                            strtotime('1th may', $year));
        $holidays[] = array('Nasjonaldag',
                            strtotime('17th may', $year));
        $holidays[] = array('Kristi himmelfart',
                            strtotime('+39 days', $easter));
        $holidays[] = array('Pinse',
                            strtotime('+49 days', $easter));
        $holidays[] = array('Andre pinsedag',
                            strtotime('+50 days', $easter));
        $holidays[] = array('Juledag',
                            strtotime('25th december', $year));
        $holidays[] = array('Andre juledag',
                            strtotime('26th december', $year));
        $year = strtotime("+1 year", $year);
    }

    return array_map(
        function($time) {
            return array(
                'title' => $time[0],
                'start' => date('c', $time[1]),
                'end' => date('c', strtotime('+1 day', $time[1])),
                'class' => 'holiday'
            ); }, $holidays);
}



function main() {
    oauth_load_saved_access_token();

    $calendars = gcal_get_calendars();
    $output = array();

    foreach ($calendars->items as $calendar) {
        $calendarColor = $calendar->backgroundColor;

        $calendar_id = $calendar->id;
        $events = gcal_get_events($calendar_id)->items;

        foreach ($events as $event) {

            $start = ifsetor($event->start->dateTime, $event->start->date);
            $end = ifsetor($event->end->dateTime, $event->end->date);
            if (isset($event->colorId)) {
                $color = googleColors($event->colorId);
            } else {
                $color = $calendarColor;
            }

            $output[] = array(
                'title' => $event->summary,
                'start' => $start,
                'end' => $end,
                'color' => $color
            );
        }
    }

    $output = array_merge($output, get_norwegian_holidays());

    header("Content-type: text/clojure");
    $output = clojure_encode($output);
    file_put_contents(CACHE, $output);
    chmod(CACHE, 0777);
    echo $output;
}

main();
