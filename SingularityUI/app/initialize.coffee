# Set up the only app globals
window.utils = require 'utils'
window.app = require 'application'

Messenger = require 'messenger'

require 'bootstrap'

vex = require 'vex'
vex.dialog = require 'vex.dialog'

apiRootPromptTemplate = require './templates/vex/apiRootPrompt'

# Set up third party configurations
require 'thirdPartyConfigurations'
# Set up the Handlebars helpers
require 'handlebarsHelpers'

# Initialize the app on DOMContentReady
$ ->
	if config.apiRoot
		app.initialize()
	else
		# In the event that the apiRoot isn't set (running through Brunch server)
		# prompt the user for it and refresh
		vex.dialog.prompt
			message: apiRootPromptTemplate()
			callback: (value) =>
				if value
					localStorage.setItem "apiRootOverride", value
				window.location = window.location.href
