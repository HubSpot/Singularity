Model = require './model'

# Not used by itself. Subclassed by Rack & Slave
class ServerItem extends Model

    removeTemplates:
        DEAD: require '../views/templates/vex/serverRemoveDead'
        DECOMISSIONING: require '../views/templates/vex/serverRemoveDecomissioned'
        DECOMISSIONED: require '../views/templates/vex/serverRemoveDecomissioned'
    decomissionTemplate: require '../views/templates/vex/serverDecomission'

    parse: (item) =>
        if item.firstSeenAt
            item.uptimeHuman = utils.humanTimeAgo(item.firstSeenAt).replace ' ago', ''

        if item.decomissioningAt?
            item.decomissioningAtHuman = utils.humanTimeAgo item.decomissioningAt

        if item.decomissionedAt?
            item.decomissionedAtHuman = utils.humanTimeAgo item.decomissionedAt

        if item.deadAt?
            item.deadAtHuman = utils.humanTimeAgo item.deadAt

        item

    decomission: =>
        $.ajax
            url: "#{ @url() }/decomission?user=#{ app.getUsername() }"
            type: "POST"

    destroy: =>
        state = @get('state')
        state = if state is 'DECOMISSIONED' then 'DECOMISSIONING' else state

        unless state?
            return new Error 'Need to know the state of a server item to remove it.'
            
            unless state in ['DECOMISSIONING', 'DEAD']
                return new Error "Can only remove dead & decommissioning slaves."
        $.ajax
            url: "#{ @url() }/#{ state.toLowerCase() }?user=#{ app.getUsername() }"
            type: "DELETE"

    # 
    # promptX pops up a user confirmation and then does what you asked of it if they approve
    #
    promptRemove: (callback) =>
        vex.dialog.confirm
            message: @removeTemplates[@get 'state'] {@id, @type}

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
                @decomission().done callback

module.exports = ServerItem
