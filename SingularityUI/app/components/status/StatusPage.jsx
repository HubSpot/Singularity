import React from 'react';
import HostStates from './HostStates';
import StatusList from './StatusList';
import Breakdown from './Breakdown';
import Link from '../common/atomicDisplayItems/Link';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import PlainText from '../common/atomicDisplayItems/PlainText';
import { connect } from 'react-redux';

export default class StatusPage extends React.Component {

  requestDetail(model) {
      let totalRequests = model.activeRequests + model.pausedRequests + model.cooldownRequests + model.pendingRequests + model.cleaningRequests;

      let requests = [
          {
              type: 'active',
              attribute: 'activeRequests',
              label: 'active',
              count: model.activeRequests,
              percent: model.activeRequests / totalRequests * 100,
              link: '/requests/active'
          },
          {
              type: 'paused',
              attribute: 'pausedRequests',
              label: 'paused',
              count: model.pausedRequests,
              percent: model.pausedRequests / totalRequests * 100,
              link: '/requests/paused'
          },
          {
              type: 'cooldown',
              attribute: 'cooldownRequests',
              label: 'cooling down',
              count: model.cooldownRequests,
              percent: model.cooldownRequests / totalRequests * 100,
              link: '/requests/cooldown'
          },
          {
              type: 'pending',
              attribute: 'pendingRequests',
              label: 'pending',
              count: model.pendingRequests,
              percent: model.pendingRequests / totalRequests * 100,
              link: '/requests/pending'
          },
          {
              type: 'cleaning',
              attribute: 'cleaningRequests',
              label: 'cleaning',
              count: model.cleaningRequests,
              percent: model.cleaningRequests / totalRequests * 100,
              link: '/requests/cleaning'
          },
      ];

      return ({
          requests,
          totalRequests
      });
  }

  taskDetail(model) {

      let totalTasks = model.activeTasks + model.lateTasks + model.scheduledTasks + model.cleaningTasks + model.lbCleanupTasks;
      let tasks = [
          {
              type: 'active',
              attribute: 'activeTasks',
              label: 'active',
              count: model.activeTasks,
              percent: model.activeTasks / totalTasks * 100,
              link: '/tasks'
          },
          {
              type: 'scheduled',
              attribute: 'scheduledTasks',
              label: 'scheduled',
              count: model.scheduledTasks,
              percent: model.scheduledTasks / totalTasks * 100,
              link: '/tasks/scheduled'
          },
          {
              type: 'overdue',
              attribute: 'lateTasks',
              label: 'overdue',
              count: model.lateTasks,
              percent: model.lateTasks / totalTasks * 100,
              link: '/tasks/scheduled'
          },
          {
              type: 'cleaning',
              attribute: 'cleaningTasks',
              label: 'cleaning',
              count: model.cleaningTasks,
              percent: model.cleaningTasks / totalTasks * 100,
              link: '/tasks/cleaning'
          },
          {
              type: 'lbCleanup',
              attribute: 'lbCleanupTasks',
              label: 'load balancer cleanup',
              count: model.lbCleanupTasks,
              percent: model.lbCleanupTasks / totalTasks * 100,
              link: '/tasks/lbcleanup'
          }
      ];

      return ({
          tasks,
          totalTasks
      });
  }

  getRequestsData(model) {
    return model.requests.map((r) => {
      return (
        {
          component: Link,
          beforeFill: r.type,
          prop: {
            text: `${r.count} ${r.label} ${this.renderPercentage(r.count, model.totalRequests)}`,
            url: `${config.appRoot}${r.link}`,
            value: r.count,
            id: r.type
          }
        }
      );
    });
  }

  getTasksData(model) {
    let res = model.tasks.map((t) => {
      return (
        {
          component: Link,
          beforeFill: t.type,
          prop: {
            text: `${t.count} ${t.label} ${this.renderPercentage(t.count, model.totalTasks)}`,
            url: `${config.appRoot}${t.link}`,
            value: t.count,
            id: t.type
          }
        }
      );
    });
    return res;
  }

  renderPercentage(number, total) {
    return number > 0 ? `(${Math.round(number/total * 100)}%)` : '';
  }

  renderTaskLag(model) {
    if (model.maxTaskLag > 0) {
      return (
        <h4>
          <TimeStamp prop={{
            timestamp: model.maxTaskLag,
            display: 'duration',
            prefix: 'Max Task Lag:'
           }} />
        </h4>
      );
    }
  }

  render() {
    let m = this.props.status;

    m.isLeaderConnected = false;
    m.hasLeader = false;
    for(let host in m.hostStates) {
      if(host.driverStatus == 'DRIVER_RUNNING') {
        m.hasLeader = true;
        if(host.mesosConnected) m.isLeaderConnected = true;
      }
    }
    _.extend(m, this.requestDetail(m));
    _.extend(m, this.taskDetail(m));

    return (
      <div>
        <div className="row">
          <div className="col-sm-12 col-md-6">
            <h2>Requests</h2>
            <div className="row">
              <div className="col-md-3 col-sm-3 hidden-xs chart">
                <Breakdown total={m.allRequests} data={m.requests} />
              </div>
              <div className="col-md-9 col-sm-9">
                <StatusList data={this.getRequestsData(m)} />
              </div>
            </div>
          </div>
          <div className="col-sm-12 col-md-6">
            <h2>Tasks</h2>
              <div className="row">
                <div className="col-md-3 col-sm-3 hidden-xs chart">
                  <Breakdown total={m.totalTasks} data={m.tasks} />
                </div>
                <div className="col-md-9 col-sm-9">
                  <StatusList data={this.getTasksData(m)} />
                  {this.renderTaskLag(m)}
                </div>
            </div>
          </div>
        </div>
        <div className="row">
          <div className="col-md-4 col-sm-12">
            <StatusList
              header="Racks"
              data={[
                {
                  component: Link,
                  prop: {
                    text: `${m.activeRacks} Active Racks`,
                    url: `${config.appRoot}/racks/active`,
                    id: 'activeracks',
                    value: m.activeRacks
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.decomissioningRacks} Decommissioning Racks`,
                    url: `${config.appRoot}/racks/decommission`,
                    id: 'decomracks',
                    value: m.decomissioningRacks
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.deadRacks} Inactive Racks`,
                    url: `${config.appRoot}/racks/inactive`,
                    id: 'inactiveracks',
                    value: m.deadRacks
                  }
                }
              ]}
            />
          </div>
          <div className="col-md-4 col-sm-12">
            <StatusList
              header="Slaves"
              data={[
                {
                  component: Link,
                  prop: {
                    text: `${m.activeSlaves} Active Slaves`,
                    url: `${config.appRoot}/slaves/active`,
                    value: m.activeSlaves,
                    id: 'activeslaves'
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.decomissioningSlaves} Decommissioning Slaves`,
                    url: `${config.appRoot}/slaves/decommission`,
                    value: m.decomissioningSlaves,
                    id: 'decomslaves'
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.deadSlaves} Inactive Slaves`,
                    url: `${config.appRoot}/slaves/inactive`,
                    className: m.deadSlaves > 0 ? 'color-warning' : '',
                    value: m.deadSlaves,
                    id: 'deadslaves'
                  }
                },
                m.unknownSlaves ? {
                  component: Link,
                  prop: {
                    text: `${m.unknownSlaves} Unknown Slaves`,
                    url: `${config.appRoot}/slaves/inactive`,
                    className: 'color-warning',
                    value: m.unknownSlaves,
                    id: 'unknownslaves'
                  }
                } : undefined
              ]}
            />
          </div>
          <div className="col-md-4 col-sm-12">
            <StatusList
              header="Deploys"
              data={[
                {
                  component: PlainText,
                  prop: {
                    text: `${m.numDeploys} Active Deploys`,
                    className: m.numDeploys < 2 ? 'text-muted' : '',
                    value: m.numDeploys,
                    id: 'numdeploys'
                  }
                },
                m.oldestDeploy != 0 ? {
                  component: TimeStamp,
                  prop: {
                      timestamp: m.oldestDeploy,
                      display: 'duration',
                      postfix: 'since last deploy'
                  }
                } : undefined
              ]}
            />
          </div>
        </div>
        <div className="row">
          <div className="col-sm-12">
            <HostStates hosts={m.hostStates} />
          </div>
        </div>
      </div>
    );
  }
}

function mapStateToProps(state) {
    return {
        status: state.api.status.data
    }
}

export default connect(mapStateToProps)(StatusPage);
