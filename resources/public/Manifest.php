<?php
require 'global.php';
header("Content-type: text/cache-manifest");
?>CACHE MANIFEST

# Version hash: <?php echo md5(file_get_contents("http://kalender.knuthelland.com/server.php")); ?>

CACHE:
index.php
<?php
if (debug_mode() > 0) {
    echo '# DEBUG MODE ON ', (int)(time()/10), "\n";
}
if (debug_mode() > 2) {
    echo 'js/main-debug.js', "\n";
} else {
    echo 'js/main.js', "\n";
}
?>
style.css
utoi.php
server.php
jquery-1.9.1.min.js


