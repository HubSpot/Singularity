var webpack = require('webpack');
var path = require('path');

dest = path.resolve(__dirname, 'dist');

module.exports = {
  entry: {
    app: './app/initialize.es6',
    vendor: [
      'react',
      'jquery',
      'underscore',
      'clipboard',
      'select2',
      'handlebars',
      'moment',
      'messenger',
      'bootstrap',
      'classnames',
      'react-interval',
      'react-dom',
      'fuzzy',
      'datatables',
      'sortable',
      'juration',
      'backbone',
      'vex-js'
    ],
  },
  output: {
    path: dest,
    filename: 'app.js'
  },
  debug: true,
  devtool: 'source-map',
  module: {
    loaders: [
      { test: /\.es6$/, exclude: /node_modules/, loader: "babel-loader" },
      { test: /\.jsx$/, exclude: /node_modules/, loader: "babel-loader" },
      { test: /\.cjsx$/, loaders: ['coffee', 'cjsx']},
      { test: /\.coffee$/, loader: 'coffee'},
      { test: /\.hbs/, loader: "handlebars-template-loader" },
      { test: /[\/]messenger\.js$/, loader: 'exports?Messenger'},
      { test: /[\/]sortable\.js$/, loader: 'exports?Sortable'}
    ]
  },
  resolve: {
    root: path.resolve('./app'),
    extensions: ['', '.js', '.es6', '.jsx', '.cjsx', '.coffee', '.hbs'],
    alias: {
      'vex': 'vex-js/js/vex.js',
      'vex.dialog': 'vex-helper.es6',
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
    }),
    new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.bundle.js')
  ]
};
