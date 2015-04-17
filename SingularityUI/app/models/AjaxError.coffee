Model = require './model'

# Used to POST new delays by the deploy form
class AjaxError extends Model
  defaults:
    status: 0
    message: '(no message)'
    present: false
    shouldRefresh: false

  setFromErrorResponse: (error) =>
    if error.responseText.match /Task .*? does not have a directory yet - check again soon/
      message = "Hang tight, this task is still starting up!"
    else
      message = error.responseText

    @set
      status: error.status
      shouldRefresh: error.status is 400
      present: true
      message: message

module.exports = AjaxError
