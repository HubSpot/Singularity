path = require 'path'
fs = require 'fs'

handlebars = require 'handlebars-brunch/node_modules/handlebars'

# Brunch settings
exports.config =
    paths:
        public: path.resolve(__dirname, '../SingularityService/src/main/resources/static')

    files:
        javascripts:
            defaultExtension: 'coffee'

            joinTo:
                'static/js/app.js': /^app/
                'static/js/vendor.js': /^vendor/

            order:
                before: [
                    'vendor/scripts/log.coffee'
                    'vendor/scripts/parseDec.coffee'
                    'vendor/scripts/modernizr.custom-2.6.2.js'
                    'vendor/scripts/jquery-1.10.2.js'
                    'vendor/scripts/jquery.cookie-1.1.js'
                    'vendor/scripts/jquery.mousewheel-3.0.6.js'
                    'vendor/scripts/jquery.swipe-1.1.0.js'
                    'vendor/scripts/underscore-1.5.1.js'
                    'vendor/scripts/underscore.string-2.3.0.js'
                    'vendor/scripts/backbone-1.0.0.js'
                    'vendor/scripts/moment-2.1.0.js'
                    'vendor/scripts/humanize-1.4.2.js'
                    'vendor/scripts/vex-2.0.2.js'
                    'vendor/scripts/tether-0.1.3.js'
                    'vendor/scripts/drop-0.1.5.js'
                    'vendor/scripts/select-0.2.0.js'
                    'vendor/scripts/backbone.paginator-0.9.0.js'
                    'vendor/scripts/teeble-0.3.6.js'
                    'vendor/scripts/bootstrap-typeahead-2.0.0.js'
                ]

        stylesheets:
            defaultExtension: 'styl'
            joinTo: 'static/css/app.css'
            order:
                before: [
                    'vendor/styles/bootstrap.css'
                    'vendor/styles/bootstrap-responsive.css'
                    'vendor/styles/docs.css'
                ]

        templates:
            defaultExtension: 'hbs'
            joinTo: 'static/js/app.js'

    server:
        base: '/singularity/v2'


    # When running SingularityUI via brunch server we need to make an index.html for it
    # bsed on the template that's shared with SingularityService
    # 
    # After we compile the static files, compile index.html using some required configs
    onCompile: =>
        destination = path.resolve @config.paths.public, 'index.html'

        templatePath = path.resolve 'app/assets/_index.mustache'
        indexTemplate = fs.readFileSync templatePath, 'utf-8'

        templateData =
            staticRoot: "#{ @config.server.base }/static"
            appRoot: @config.server.base
            apiRoot: ""
            mesosLogsPort: 5051

        compiledTemplate = handlebars.compile(indexTemplate)(templateData)
        fs.writeFileSync destination, compiledTemplate
