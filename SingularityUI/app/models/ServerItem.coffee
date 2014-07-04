Model = require './model'

# Not used by itself. Subclassed by Rack & Slave
class ServerItem extends Model

    removeTemplate: require '../views/templates/vex/serverRemoveDead'
    decomissionTemplate: require '../views/templates/vex/serverDecomission'

    parse: (item) =>
        if item.firstSeenAt
            item.uptimeHuman = utils.humanTimeAgo(item.firstSeenAt).replace ' ago', ''

        if item.decomissioningAt?
            item.decommissioningAtHuman = utils.humanTimeAgo item.decomissioningAt

        if item.decomissionedAt? and item.deadAt?
            item.decommissionedAtHuman = utils.humanTimeAgo item.decommissionedAt
            item.deadAthuman = utils.humanTimeAgo item.deadAt

        item

    decomission: =>
        $.ajax
            url: "#{ @url() }/decomission?user=#{ app.getUsername() }"
            type: "POST"

    destroy: =>
        state = @get('state')
        unless state?
            return new Error 'Need to know the state of a server item to remove it.'
            
            unless state in ['DECOMISSIONED', 'DEAD']
                return new Error "Can't remove an active server item. Decommission first." 

        console.log "#{ @url() }/#{@get('state').toLowerCase()}?user=#{app.getUsername()}"
        return
        $.ajax
            url: "#{ @url() }/#{@get('state').toLowerCase()}?user=#{app.getUsername()}"
            type: "DELETE"

    # 
    # promptX pops up a user confirmation and then does what you asked of it if they approve
    #
    promptRemove: (callback) =>
        vex.dialog.confirm
            message: @removeTemplate {@id, @type}

            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Remove',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            callback: (confirmed) =>
                return unless confirmed
                @destroy().done callback

    promptDecommission: (callback) =>
        vex.dialog.confirm
            message: @decomissionTemplate {@id, @type}

            buttons: [
                $.extend {}, vex.dialog.buttons.YES,
                    text: 'Decommission',
                    className: 'vex-dialog-button-primary vex-dialog-button-primary-remove'
                vex.dialog.buttons.NO
            ]

            callback: (data) =>
                return if data is false
                console.log "Decomissioning"
                @decomission().done callback

module.exports = ServerItem
