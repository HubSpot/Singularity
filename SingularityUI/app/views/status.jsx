import View from './view';
import StatusPage from '../components/status/StatusPage';

import React from 'react';
import ReactDOM from 'react-dom';

class StatusView extends View {

    constructor(...args) {
        super(...args);
        this.render = this.render.bind(this);
    }

    initialize() {
        return this.listenTo(this.model, 'sync', this.render);
    }

    render() {
        if (!this.model.synced) {
          return;
        }

        let isLeaderConnected = false;
        let hasLeader = false;
        for (let i = 0; i < this.model.attributes.hostStates.length; i++) {
            let host = this.model.attributes.hostStates[i];
            if (host.driverStatus === 'DRIVER_RUNNING') {
                hasLeader = true;
                if (host.mesosConnected) {
                    isLeaderConnected = true;
                }
            }
        }

        ReactDOM.render(<StatusPage model={this.model.attributes} hasLeader={hasLeader} isLeaderConnected={isLeaderConnected} />, this.el)
        // this.$el.html(this.template({
        //     state: this.model.toJSON(),
        //     synced: this.model.synced,
        //     tasks: this.model.taskDetail().tasks,
        //     requests: this.model.requestDetail().requests,
        //     totalRequests: this.model.requestDetail().total,
        //     totalTasks: this.model.taskDetail().total,
        //     hasLeader,
        //     isLeaderConnected
        // }));

        // if @lastState?
        //     changedNumbers = {}
        //     numberAttributes = []
        //     # Go through each key. If the value is a number, we'll (try to)
        //     # perform a change animation on that key's box
        //     _.each _.keys(@model.attributes), (attribute) =>
        //         if typeof @model.attributes[attribute] is 'number'
        //             numberAttributes.push attribute
        //
        //     for numberAttribute in numberAttributes
        //         oldNumber = @lastState[numberAttribute]
        //         newNumber = @model.attributes[numberAttribute]
        //         if oldNumber isnt newNumber
        //             changedNumbers[numberAttribute] =
        //                 direction: "#{ if newNumber > oldNumber then 'inc' else 'dec' }rease"
        //                 difference: "#{ if newNumber > oldNumber then '+' else '-' }#{ Math.abs(newNumber - oldNumber) }"
        //
        //     for attributeName, changes of changedNumbers
        //         changeClassName = "changed-direction-#{ changes.direction }"
        //         $attribute = @$el.find("""[data-state-attribute="#{ attributeName }"]""").not('[data-type="column"]')
        //         $bigNumber = $attribute.closest('.list-group-item')
        //         $bigNumber.find('a').addClass(changeClassName).append("<span class='changeDifference'>#{changes.difference}</span>")
        //         $attribute.html @model.attributes[attributeName]
        //
        //         do ($bigNumber, changeClassName) ->
        //             setTimeout (->
        //                 $bigNumber.find('a').removeClass(changeClassName)
        //                           .find('changeDifference').remove().end()
        //                           .find('.changeDifference').fadeOut(1500)
        //             ), 2500
        //
        // this.$('.chart .chart__data-point[title]').tooltip({placement: 'right'});
        // this.captureLastState();
    }
}

export default StatusView;
