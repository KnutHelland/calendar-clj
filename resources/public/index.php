<?php
require 'global.php';
if (debug_mode() > 1) {
    echo '<html>';
} else {
    echo '<html manifest="Manifest.php">';
}

$script = 'main.js';
if (debug_mode() > 2) {
    $script = 'main-debug.js';
}
?>
  <head>
    <title>Test</title>

    <meta name="viewport" content="user-scalable=no, width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black" />
    <link rel="apple-touch-icon" href="icon.png"/>
    <link rel="apple-touch-startup-image" href="startup-image.png" />

    <script src="jquery-1.9.1.min.js"></script>
    <script type="text/javascript" src="js/<?php echo $script; ?>"></script>
    <link href="style.css" type="text/css" rel="stylesheet" />
  </head>
  <body onload="kh.calendar.init('#calendar');">
    <div id="calendar"></div>
    <script>
      /*
       * Automatically reloads the calendar if newer version available.
       */
      window.applicationCache.addEventListener("updateready", function(e) {
        if (window.applicationCache.status == window.applicationCache.UPDATEREADY) {
          window.applicationCache.swapCache();
          window.location.reload();
        }
      });
    </script>
  </body>
</html>
