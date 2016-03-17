Model = require './model'
moment = require 'moment'
vex = require 'vex.dialog'

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

    remove: (message) =>
        data = {}
        if message
            data.message = message
        $.ajax
            url: @url()
            type: "DELETE"
            contentType: 'application/json'
            data: JSON.stringify(data)

    freeze: (message) =>
        data = {}
        if message
            data.message = message
        $.ajax
            url: "#{ @url() }/freeze"
            type: "POST"
            contentType: 'application/json'
            data: JSON.stringify(data)

    decommission: (message) =>
        data = {}
        if message
            data.message = message
        $.ajax
            url: "#{ @url() }/decommission"
            type: "POST"
            contentType: 'application/json'
            data: JSON.stringify(data)

    reactivate: (message) =>
        data = {}
        if message
            data.message = message
        $.ajax
            url: "#{ @url()}/activate"
            type: "POST"
            contentType: 'application/json'
            data: JSON.stringify(data)

    host: =>
        @get 'host'

    #
    # promptX pops up a user confirmation and then does what you asked of it if they approve
    #

    promptRemove: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @removeTemplates[state] {@id, @host, @type}
            input: """
                <input name="message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Remove',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            callback: (data) =>
                return unless data
                @remove(data.message).done callback

    promptFreeze: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @freezeTemplate {@id, @host, @type}
            input: """
                <input name="message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Freeze',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (data) =>
                return unless data
                @freeze(data.message).done callback

    promptDecommission: (callback) =>
        state = @get 'state'
        vex.dialog.open
            message: @decommissionTemplate {@id, @host, @type}
            input: """
                <input name="message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Decommission',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (data) =>
                return unless data
                @decommission(data.message).done callback


    promptReactivate: (callback) =>
        state = @get 'state'
        vex.dialog.confirm
            message: @reactivateTemplate {@id, @host, @type}
            input: """
                <input name="message" type="text" placeholder="Message (optional)" />
            """
            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Reactivate',
                    className: 'vex-dialog-button-primary'
                vex.dialog.buttons.NO
            ]

            callback: (data) =>
                return unless data
                @reactivate(data.message).done callback


module.exports = ServerItem
