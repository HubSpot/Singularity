var gulp = require('gulp');
var path = require('path');
var brunch = require('brunch');
var del = require('del');

gulp.task("clean", function() {
  return del([
    path.resolve(__dirname, '../SingularityService/target/generated-resources/assets/static/**'),
    path.resolve(__dirname, '../SingularityService/target/generated-resources/assets/index.html')], {force: true});
});

gulp.task("build", function(cb) {
  brunch.build({}, function () {
    cb();
  });
});

gulp.task("default", ["clean", "build"])