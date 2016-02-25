var webpack = require('webpack');
var path = require('path');

module.exports = function (dest) {
  return {
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
        'vex': 'vex-js/js/vex.js',
        'vex.dialog': 'vex-helper.coffee',
        'handlebars': 'handlebars/runtime.js',
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
  };
};
