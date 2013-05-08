<?php

/**
 * Gets or sets the debug mode
 */
function describe_debug($mode) {
    switch ($mode) {
    case 0:
    case false:
        return 'Production mode';
        break;
    case 1:
        return 'Change cache manifest every 10 seconds';
        break;
    case 2:
        return 'Remove manifest from html';
        break;
    case 3:
        return 'Swap js to debug version.';
        break;
    default:
        return false;
    }
}

function debug_mode($mode = null) {
    if (!is_null($mode) && describe_debug($mode)) {
        file_put_contents('.debug', $mode);
        chmod('.debug', 0777);
        return $mode;
    }
    
    if (!is_null($mode) && $mode === false) {
        unlink(".debug");
    }

    if (file_exists(".debug")) {
        return file_get_contents(".debug");
    }
    return false;
}


/*
 * Usage logs
 */

// Touch logfile:
if (!file_exists('.log')) {
    file_put_contents('.log', '');
    chmod('.log', 0777);
}

// Logging vars:
$ip = $_SERVER['REMOTE_ADDR'];
$datestamp = date('c');
$request = $_SERVER['REQUEST_URI'];

// Read previous logged item from this location:
$lines = file('.log');

$doWriteLog = true;

foreach ($lines as $line) {
    list($time, $addr) = preg_split("/(\s)+/i", $line);
    if ($ip == $addr) {
        $time = strtotime($time);
        if ($time > (time() - 3600)) {
            $doWriteLog = false;
        }
        break;
    }
}

// Write to log:
if ($doWriteLog) {
    $apiKey = '6dbeb858bcf6df61fb7f2f8819b4936f79f46d1a26dbfe20467173f42009fe91';
    $url = 'http://api.ipinfodb.com/v3/ip-city/?key='.$apiKey.'&ip='.$ip;
    $location = file_get_contents($url);

    $fp = fopen('.log', 'w');
    fwrite($fp, $datestamp."\t".$ip."\t".$location."\n");
    fwrite($fp, implode('', $lines));
    fclose($fp);
}
