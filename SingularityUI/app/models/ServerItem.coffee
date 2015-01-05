Model = require './model'

# Not used by itself. Subclassed by Rack & Slave
class ServerItem extends Model

    removeTemplates:
        DEAD:                  require '../templates/vex/serverRemoveDead'
        MISSING_ON_STARTUP:    require '../templates/vex/serverRemoveDead'
        STARTING_DECOMMISSION: require '../templates/vex/serverRemoveDecomissioned'
        DECOMMISSIONING:       require '../templates/vex/serverRemoveDecomissioned'
        DECOMMISSIONED:        require '../templates/vex/serverRemoveDecomissioned'
        ACTIVE:                require '../templates/vex/serverDecomission'

    reactivateTemplate:
        require '../templates/vex/slaveReactivate'

    parse: (item) =>
        if item.firstSeenAt?
            if item.decomissioningAt?
                item.uptime = item.decomissioningAt - item.firstSeenAt
            else if item.deadAt?
                item.uptime = item.deadAt - item.firstSeenAt
            else
                item.uptime = moment() - item.firstSeenAt
        if item.currentState?
            item.state = item.currentState.state
        item

    destroy: =>
        $.ajax
            url: "#{ @url() }?user=#{ app.getUsername() }"
            type: "DELETE"

    reactivate: =>
        $.ajax
            url: "#{ @url()}/activate?user=#{ app.getUsername()}"
            type: "POST"

    #
    # promptX pops up a user confirmation and then does what you asked of it if they approve
    #

    promptRemove: (callback) =>
        state = @get 'state'
        text = if state is 'ACTIVE' then 'Decommission' else 'Remove'
        vex.dialog.confirm
            message: @removeTemplates[state] {@id, @type}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: text,
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @destroy().done callback

    promptReactivate: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @reactivateTemplate {@id}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Reactivate',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @destroy().done callback


module.exports = ServerItem
