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
        this.listenTo(this.model, 'sync', this.render);
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

        ReactDOM.render(<StatusPage
          model={this.model.attributes}
          hasLeader={hasLeader}
          isLeaderConnected={isLeaderConnected}
          requests={this.model.requestDetail().requests}
          tasks={this.model.taskDetail().tasks}
          totalTasks={this.model.taskDetail().total}
          />, this.el)
    }
}

export default StatusView;
