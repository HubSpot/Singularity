var webpack = require('webpack');
var WebpackDevServer = require('webpack-dev-server');
var config = require('./webpack.config');

var SINGULARITY_API_ROOT = process.env.SINGULARITY_API_ROOT;

new WebpackDevServer(webpack(config), {
  publicPath: config.output.publicPath,
  hot: true,
  proxy: {
    '/singularity/api/*': {
      target: SINGULARITY_API_ROOT,
      rewrite: function(req) {
        req.url = req.url.replace(/^\/singularity\/api/, '');
      }
    }
  },
  historyApiFallback: true
}).listen(3223, 'localhost', function (err, result) {
  if (err) {
    return console.log(err);
  }

  console.log('Listening at http://localhost:3223/');
});
