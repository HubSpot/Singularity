Model = require './model'

# Not used by itself. Subclassed by Rack & Slave
class ServerItem extends Model

    removeTemplates:
        DEAD:                  require '../templates/vex/serverRemoveDead'
        MISSING_ON_STARTUP:    require '../templates/vex/serverRemoveDead'
        STARTING_DECOMISSION:  require '../templates/vex/serverRemoveDecomissioned'
        STARTING_DECOMMISSION: require '../templates/vex/serverRemoveDecomissioned'
        DECOMMISSIONING:       require '../templates/vex/serverRemoveDecomissioned'
        DECOMISSIONING:        require '../templates/vex/serverRemoveDecomissioned'
        DECOMMISSIONED:        require '../templates/vex/serverRemoveDecomissioned'
        DECOMISSIONED:         require '../templates/vex/serverRemoveDecomissioned'
        ACTIVE:                require '../templates/vex/serverDecomission'

    decommissionTemplate:
        require '../templates/vex/serverDecomission'

    freezeTemplate:
        require '../templates/vex/serverFreeze'

    reactivateTemplate:
        require '../templates/vex/slaveReactivate'

    parse: (item) =>
        if item instanceof Array
            current = {}
            current.timestamp = 0
            for i in item
                current = i if i.timestamp > current.timestamp
            item = current
        if item.firstSeenAt?
            if item.decomissioningAt?
                item.uptime = item.decomissioningAt - item.firstSeenAt
            else if item.deadAt?
                item.uptime = item.deadAt - item.firstSeenAt
            else
                item.uptime = moment() - item.firstSeenAt
        if item.currentState?
            item.state = item.currentState.state
            item.user = item.currentState.user
        item

    remove: =>
        $.ajax
            url: "#{ @url() }?user=#{ app.getUsername() }"
            type: "DELETE"

    freeze: =>
        $.ajax
            url: "#{ @url() }/freeze?user=#{ app.getUsername() }"
            type: "POST"

    decommission: =>
        $.ajax
            url: "#{ @url() }/decommission?user=#{ app.getUsername() }"
            type: "POST"

    reactivate: =>
        $.ajax
            url: "#{ @url()}/activate?user=#{ app.getUsername()}"
            type: "POST"

    host: =>
        @get 'host'

    #
    # promptX pops up a user confirmation and then does what you asked of it if they approve
    #

    promptRemove: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @removeTemplates[state] {@id, @host, @type}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Remove',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @remove().done callback

    promptFreeze: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @freezeTemplate {@id, @host, @type}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Freeze',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @freeze().done callback

    promptDecommission: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @decommissionTemplate {@id, @host, @type}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Decommission',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @decommission().done callback


    promptReactivate: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @reactivateTemplate {@id, @host, @type}
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Reactivate',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @reactivate().done callback


module.exports = ServerItem
