var gulp = require('gulp');
var path = require('path');
var del = require('del');
var child_process = require('child_process');

var dest = path.resolve(__dirname, '../SingularityService/target/generated-resources/assets');

gulp.task("clean", function() {
  return del([
    path.resolve(dest, 'static/**'),
    path.resolve(dest, 'index.html')], {force: true});
});

gulp.task("build", function(cb) {
  var brunch = child_process.execFile('node_modules/brunch/bin/brunch', ['build', '--production']);

  var hasStderrOutput = false;

  brunch.stdout.pipe(process.stdout);
  brunch.stderr.pipe(process.stderr);

  brunch.stderr.on('data', function () {
    hasStderrOutput = true;
  });

  brunch.on('error', cb);

  brunch.on('exit', function (code) {
    if (hasStderrOutput) {
      cb(new Error("Brunch build failed"));
    } else if (code != 0) {
      cb(new Error("Brunch exited with code " + code));
    } else {
      cb();
    }
  });
});

gulp.task("default", ["clean", "build"]);
