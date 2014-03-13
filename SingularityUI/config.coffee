exports.config =

    paths:

        public: '../SingularityService/src/main/resources/static'

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
        base: '/singularity'
