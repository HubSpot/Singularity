Model = require './model'

# Not used by itself. Subclassed by Rack & Slave
class ServerItem extends Model

    removeTemplates:
        DEAD: require '../templates/vex/serverRemoveDead'
        DECOMISSIONING: require '../templates/vex/serverRemoveDecomissioned'
        DECOMISSIONED: require '../templates/vex/serverRemoveDecomissioned'
    decomissionTemplate: require '../templates/vex/serverDecomission'

    parse: (item) =>
        if item.firstSeenAt?
            if item.decomissioningAt?
                item.uptime = item.decomissioningAt - item.firstSeenAt
                item.uptimeHuman = utils.humanTime(item.firstSeenAt, item.decomissioningAt)
            else if item.deadAt?
                item.uptime = item.deadAt - item.firstSeenAt
                item.uptimeHuman = utils.humanTime(item.firstSeenAt, item.deadAt)
            else
                item.uptime = moment() - item.firstSeenAt
                item.uptimeHuman = utils.humanTime(item.firstSeenAt)

            item.uptimeHuman = item.uptimeHuman?.replace(' ago', '')

        if item.decomissioningAt?
            item.decomissioningAtHuman = utils.humanTime item.decomissioningAt

        if item.decomissionedAt?
            item.decomissionedAtHuman = utils.humanTime item.decomissionedAt

        if item.deadAt?
            item.deadAtHuman = utils.humanTime item.deadAt

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
