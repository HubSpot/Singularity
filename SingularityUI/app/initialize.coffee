# Set up the only app globals
window.utils = require 'utils'
window.constants = require 'constants'
window.app = require 'application'

apiRootPromptTemplate = require './views/templates/vex/apiRootPrompt'

# Set up third party configurations
require 'thirdPartyConfigurations'

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
					localStorage.setItem "apiRoot", value
				window.location = window.location.href
