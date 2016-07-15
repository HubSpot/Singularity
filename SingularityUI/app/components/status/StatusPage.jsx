import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { FetchSingularityStatus } from '../../actions/api/state';

import HostStates from './HostStates';
import StatusList from './StatusList';
import Breakdown from './Breakdown';
import Utils from '../../utils';

class StatusPage extends React.Component {

  static propTypes = {
    fetchStatus: React.PropTypes.func.isRequired,
    status: React.PropTypes.object
  }

  requestDetail(model) {
    const totalRequests = model.activeRequests + model.pausedRequests + model.cooldownRequests + model.pendingRequests + model.cleaningRequests;

    const requests = [
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
    const totalTasks = model.activeTasks + model.lateTasks + model.scheduledTasks + model.cleaningTasks + model.lbCleanupTasks;
    const tasks = [
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
    return model.requests.map((request) => {
      return (
        {
          component: (className) => (
            <Link to={request.link} className={className}>
              {request.count} {request.label} {this.renderPercentage(request.count, model.totalRequests)}
            </Link>
          ),
          beforeFill: request.type,
          value: request.count,
          id: request.type
        }
      );
    });
  }

  getTasksData(model) {
    const res = model.tasks.map((task) => {
      return (
      {
        component: (className) => (
          <Link to={task.link} className={className}>
            {task.count} {task.label} {this.renderPercentage(task.count, model.totalTasks)}
          </Link>
        ),
        beforeFill: task.type,
        value: task.count,
        id: task.type
      }
      );
    });
    return res;
  }

  renderPercentage(number, total) {
    return number > 0 && `(${Math.round(number / total * 100)}%)`;
  }

  renderTaskLag(model) {
    return model.maxTaskLag > 0 && (<h4>Max Task Lag: {Utils.duration(model.maxTaskLag)}</h4>);
  }

  render() {
    const m = this.props.status;

    m.isLeaderConnected = false;
    m.hasLeader = false;
    for (const host in m.hostStates) {
      if (host.driverStatus === 'DRIVER_RUNNING') {
        m.hasLeader = true;
        if (host.mesosConnected) m.isLeaderConnected = true;
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
                  component: (className) => (
                    <Link to="racks/active" className={className}>
                      {m.activeRacks} Active Racks
                    </Link>
                  ),
                  id: 'activeracks',
                  value: m.activeRacks
                },
                {
                  component: (className) => (
                    <Link to="racks/decommission" className={className}>
                      {m.decomissioningRacks} Decommissioning Racks
                    </Link>
                  ),
                  id: 'decomracks',
                  value: m.decomissioningRacks
                },
                {
                  component: (className) => (
                    <Link to="racks/inactive" className={className}>
                      {m.deadRacks} Inactive Racks
                    </Link>
                  ),
                  id: 'inactiveracks',
                  value: m.deadRacks
                }
              ]}
            />
          </div>
          <div className="col-md-4 col-sm-12">
            <StatusList
              header="Slaves"
              data={[
                {
                  component: (className) => (
                    <Link to="slaves/active" className={className}>
                      {m.activeSlaves} Active Slaves
                    </Link>
                  ),
                  value: m.activeSlaves,
                  id: 'activeslaves'
                },
                {
                  component: (className) => (
                    <Link to="slaves/decommission" className={className}>
                      {m.decomissioningSlaves} Decommissioning Slaves
                    </Link>
                  ),
                  value: m.decomissioningSlaves,
                  id: 'decomslaves'
                },
                {
                  component: (className) => (
                    <Link to="slaves/inactive" className={className}>
                      {m.deadSlaves} Inactive Slaves
                    </Link>
                  ),
                  className: m.deadSlaves > 0 ? 'color-warning' : '',
                  value: m.deadSlaves,
                  id: 'deadslaves'
                },
                m.unknownSlaves ? {
                  component: (className) => (
                    <Link to="slaves/inactive" className={className}>
                      {m.unknownSlaves} Unknown Slaves
                    </Link>
                  ),
                  className: 'color-warning',
                  value: m.unknownSlaves,
                  id: 'unknownslaves'
                } : null
              ]}
            />
          </div>
          <div className="col-md-4 col-sm-12">
            <StatusList
              header="Deploys"
              data={[
                {
                  component: (className) => (
                    <span className={classNames(className, m.numDeploys < 2 && 'text-muted')}>
                      <strong>{m.numDeploys}</strong> Active Deploys
                    </span>
                  ),
                  value: m.numDeploys,
                  id: 'numdeploys'
                },
                m.oldestDeploy !== 0 ? {
                  component: (className) => (
                    <span className={className}>
                      <strong>{Utils.duration(m.oldestDeploy)}</strong> since last deploy
                    </span>
                  )
                } : null
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

function mapDispatchToProps(dispatch) {
  return {
    fetchStatus: () => dispatch(FetchSingularityStatus.trigger())
  };
}

function mapStateToProps(state) {
  return {
    status: state.api.status.data
  };
}

function refresh(props) {
  return props.fetchStatus();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(StatusPage, 'Status', refresh));
