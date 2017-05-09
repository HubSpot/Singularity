var webpack = require('webpack');
var WebpackDevServer = require('webpack-dev-server');
var config = require('./webpack.config');
var path = require('path');
var express = require('express');
var proxy = require('http-proxy-middleware');


var SINGULARITY_API_ROOT = process.env.SINGULARITY_API_ROOT;

var server = new WebpackDevServer(webpack(config), {
  publicPath: config.output.publicPath,
  hot: true,
  historyApiFallback: {
    rewrites: [
      {
        from: /^\/(?!singularity\/api|static).*$/,
        to: function() {
          return 'index.html';
        }
      }
    ]
  },
  proxy: {
    '/singularity/api/*': {
      target: SINGULARITY_API_ROOT,
      changeOrigin: true,
      pathRewrite: {
        '^/singularity/api/': ''
      }
    }
  }
});

server.listen(3223, 'localhost', function (err, result) {
  if (err) {
    return console.log(err);
  }

  console.log('Listening at http://localhost:3223/');
});
