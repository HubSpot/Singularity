var gulp = require('gulp');
var path = require('path');
var del = require('del');
var child_process = require('child_process');

var mustache = require('gulp-mustache');
var stylus = require('gulp-stylus');
var nib = require('nib');

var concat = require('gulp-concat');

var connect = require('gulp-connect');

var serverBase = process.env.SINGULARITY_BASE_URI || ''

var templateData = {
  staticRoot: process.env.SINGULARITY_STATIC_URI || (serverBase + '/static'),
  appRoot: process.env.SINGULARITY_APP_URI || (serverBase + '/ui'),
  apiRoot: process.env.SINGULARITY_API_URI || '',
  slaveHttpPort: process.env.SINGULARITY_SLAVE_HTTP_PORT || 5051,
  title: process.env.SINGULARITY_TITLE || 'Singularity (local dev)',
  navColor: process.env.SINGULARITY_NAV_COLOR,
  defaultCpus: process.env.SINGUALRITY_DEFAULT_CPUS || 1,
  defaultMemory: process.env.SINGULARITY_DEFAULT_MEMORY || 128,
  defaultBounceExpirationMinutes: process.env.SINGULARITY_DEFAULT_BOUNCE_EXPIRATION_MINUTES || 60,
  defaultHealthcheckIntervalSeconds: process.env.SINGULARITY_DEFAULT_HEALTHCHECK_INTERVAL_SECONDS || 5,
  defaultHealthcheckTimeoutSeconds: process.env.SINGULARITY_HEALTHCHECK_TIMEOUT_SECONDS || 5,
  defaultDeployHealthTimeoutSeconds: process.env.SINGULARITY_DEPLOY_HEALTH_TIMEOUT_SECONDS || 120,
  defaultHealthcheckMaxRetries: process.env.SINGULARITY_HEALTHCHECK_MAX_RETRIES || 0,
  hideNewDeployButton: process.env.SINGULARITY_HIDE_NEW_DEPLOY_BUTTON || "false",
  hideNewRequestButton: process.env.SINGULARITY_HIDE_NEW_REQUEST_BUTTON || "false",
  loadBalancingEnabled: process.env.SINGULARITY_LOAD_BALANCING_ENABLED || "false",
  runningTaskLogPath:  process.env.SINGULARITY_RUNNING_TASK_LOG_PATH || "stdout",
  finishedTaskLogPath: process.env.SINGULARITY_FINISHED_TASK_LOG_PATH || "stdout",
  commonHostnameSuffixToOmit: process.env.SINGULARITY_COMMON_HOSTNAME_SUFFIX_TO_OMIT || "",
  taskS3LogOmitPrefix: process.env.SINGULARITY_TASK_S3_LOG_OMIT_PREFIX || '',
  warnIfScheduledJobIsRunningPastNextRunPct: process.env.SINGULARITY_WARN_IF_SCHEDULED_JOB_IS_RUNNING_PAST_NEXT_RUN_PCT || 200,
  shellCommands: process.env.SINGULARITY_SHELL_COMMANDS || "[]",
  timestampFormat: process.env.SINGULARITY_TIMESTAMP_FORMAT || 'lll',
  timestampWithSecondsFormat: process.env.SINGULARITY_TIMESTAMP_WITH_SECONDS_FORMAT || 'lll:ss'
}

var dest = path.resolve(__dirname, '../SingularityService/target/generated-resources/assets');

var gwebpack = require('gulp-webpack');
var webpack = require('webpack');
var webpackConfig = {
  entry: './app/initialize.coffee',
  output: {
    path: dest,
    filename: 'app.js'
  },
  debug: true,
  module: {
    loaders: [
      { test: /\.cjsx$/, loaders: ['coffee', 'cjsx']},
      { test: /\.coffee$/, loader: 'coffee'},
      { test: /\.hbs/, loader: "handlebars-template-loader" },
      { test: /[\/]messenger\.js$/, loader: 'exports?Messenger'},
      { test: /[\/]sortable\.js$/, loader: 'exports?Sortable'}
    ]
  },
  resolve: {
    root: path.resolve('./app'),
    extensions: ['', '.js', '.cjsx', '.coffee', '.hbs'],
    alias: {
      'handlebars': 'handlebars/runtime.js',
      'vex': 'vex-js/js/vex.js',
      'vex.dialog': 'vex-js/js/vex.dialog.js',
      'sortable': 'sortable/js/sortable.js',
      'datatables': 'datatables/media/js/jquery.dataTables.js',
      'bootstrap': 'bootstrap/dist/js/bootstrap.js'
    }
  },
  plugins: [
    new webpack.ProvidePlugin({
      $: 'jquery',
      '_': 'underscore',
      jQuery: 'jquery',
      'window.jQuery': 'jquery'
    })
  ]
}

gulp.task("clean", function() {
  return del([
    path.resolve(dest, 'static/**'),
    path.resolve(dest, 'index.html')], {force: true});
});

gulp.task('scripts', function () {
  return gulp.src(webpackConfig.entry)
    .pipe(gwebpack(webpackConfig))
    .pipe(gulp.dest(dest + '/static/js'))
});

gulp.task('html', function () {
  return gulp.src('app/assets/index.mustache')
    .pipe(mustache(templateData, {extension: '.html'}))
    .pipe(gulp.dest(dest))
});

gulp.task('styles', function () {
  return gulp.src('app/**/*.styl')
    .pipe(stylus({
      use: nib(),
      'include css': true
    }))
    .pipe(concat('app.css'))
    .pipe(gulp.dest(dest + '/static/css'))
})

gulp.task('build', ['clean'], function () {
  gulp.start(['scripts', 'html', 'styles']);
});

gulp.task('serve', ['scripts', 'html', 'styles'], function () {
  connect.server({
    root: dest,
    port: 3334,
    fallback: dest + '/index.html'
  })
})

gulp.task('watch', function () {
  gulp.watch('app/**/*.styl', ['styles'])
  gulp.watch('app/**/*.coffee', ['scripts'])
})

gulp.task("default", ["clean", "serve", "watch"]);
