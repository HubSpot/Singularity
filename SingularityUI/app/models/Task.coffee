Model = require './model'

Request = require './Request'

killTemplate = require '../views/templates/vex/taskKill'

class Task extends Model

    url: => "#{ config.apiRoot }/tasks/task/#{ @get 'id' }"

    ###
    promptX opens a dialog asking the user to confirm an action and then does it
    ###
    promptRun: (callback) =>
        # We tell the Request to run
        requestModel = new Request id: @get 'requestId'
        requestModel.promptRun => callback()

    promptKill: (callback) =>
        vex.dialog.confirm
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Kill task'
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            message: killTemplate id: @get('id')
            callback: (confirmed) =>
                return unless confirmed
                @destroy().done callback

module.exports = Task
