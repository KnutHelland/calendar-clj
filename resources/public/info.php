<?php

require 'global.php';

/* Switch debug mode */
if (isset($_GET['debug_mode'])) {
    if ($_GET['debug_mode'] == 0) {
        debug_mode(false);
    } else {
        debug_mode($_GET['debug_mode']);
    }

    header("location: info.php");
    exit();
}

/* Print current debug mode */
echo 'Current debug mode: '.describe_debug(debug_mode());

echo 'Set debug mode:<br />';
for ($i = 0; $i < 10; $i++) {
    if (!describe_debug($i)) {
        break;
    }

    echo '<a href="?debug_mode='.$i.'">'.describe_debug($i).'</a><br />';
}


/* Dump log */
echo '<h2>Log<h2><pre>';
echo file_get_contents(".log");