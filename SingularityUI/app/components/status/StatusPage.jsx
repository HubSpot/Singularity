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

  requestDetail(status) {
    const totalRequests = status.activeRequests + status.pausedRequests + status.cooldownRequests + status.pendingRequests + status.cleaningRequests;

    const requests = [
      {
        type: 'active',
        attribute: 'activeRequests',
        label: 'active',
        count: status.activeRequests,
        percent: status.activeRequests / totalRequests * 100,
        link: '/requests/active'
      },
      {
        type: 'paused',
        attribute: 'pausedRequests',
        label: 'paused',
        count: status.pausedRequests,
        percent: status.pausedRequests / totalRequests * 100,
        link: '/requests/paused'
      },
      {
        type: 'cooldown',
        attribute: 'cooldownRequests',
        label: 'cooling down',
        count: status.cooldownRequests,
        percent: status.cooldownRequests / totalRequests * 100,
        link: '/requests/cooldown'
      },
      {
        type: 'pending',
        attribute: 'pendingRequests',
        label: 'pending',
        count: status.pendingRequests,
        percent: status.pendingRequests / totalRequests * 100,
        link: '/requests/pending'
      },
      {
        type: 'cleaning',
        attribute: 'cleaningRequests',
        label: 'cleaning',
        count: status.cleaningRequests,
        percent: status.cleaningRequests / totalRequests * 100,
        link: '/requests/cleaning'
      },
    ];

    return ({
      requests,
      totalRequests
    });
  }

  taskDetail(status) {
    const totalTasks = status.activeTasks + status.lateTasks + status.scheduledTasks + status.cleaningTasks + status.lbCleanupTasks;
    const tasks = [
      {
        type: 'active',
        attribute: 'activeTasks',
        label: 'active',
        count: status.activeTasks,
        percent: status.activeTasks / totalTasks * 100,
        link: '/tasks'
      },
      {
        type: 'scheduled',
        attribute: 'scheduledTasks',
        label: 'scheduled',
        count: status.scheduledTasks,
        percent: status.scheduledTasks / totalTasks * 100,
        link: '/tasks/scheduled'
      },
      {
        type: 'overdue',
        attribute: 'lateTasks',
        label: 'overdue',
        count: status.lateTasks,
        percent: status.lateTasks / totalTasks * 100,
        link: '/tasks/scheduled'
      },
      {
        type: 'cleaning',
        attribute: 'cleaningTasks',
        label: 'cleaning',
        count: status.cleaningTasks,
        percent: status.cleaningTasks / totalTasks * 100,
        link: '/tasks/cleaning'
      },
      {
        type: 'lbCleanup',
        attribute: 'lbCleanupTasks',
        label: 'load balancer cleanup',
        count: status.lbCleanupTasks,
        percent: status.lbCleanupTasks / totalTasks * 100,
        link: '/tasks/lbcleanup'
      }
    ];

    return ({
      tasks,
      totalTasks
    });
  }

  getRequestsData(status) {
    return status.requests.map((request) => {
      return (
        {
          component: (className) => (
            <Link to={request.link} className={className}>
              {request.count} {request.label} {this.renderPercentage(request.count, status.totalRequests)}
            </Link>
          ),
          beforeFill: request.type,
          value: request.count,
          id: request.type
        }
      );
    });
  }

  getTasksData(status) {
    const res = status.tasks.map((task) => {
      return (
      {
        component: (className) => (
          <Link to={task.link} className={className}>
            {task.count} {task.label} {this.renderPercentage(task.count, status.totalTasks)}
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

  renderTaskLag(status) {
    return status.maxTaskLag > 0 && (<h4>Max Task Lag: {Utils.duration(status.maxTaskLag)}</h4>);
  }

  render() {
    const status = Utils.deepClone(this.props.status);

    status.isLeaderConnected = false;
    status.hasLeader = false;
    for (const host in status.hostStates) {
      if (host.driverStatus === 'DRIVER_RUNNING') {
        status.hasLeader = true;
        if (host.mesosConnected) status.isLeaderConnected = true;
      }
    }
    _.extend(status, this.requestDetail(status));
    _.extend(status, this.taskDetail(status));

    return (
      <div>
        <div className="row">
          <div className="col-sm-12 col-md-6">
            <h2>Requests</h2>
            <div className="row">
              <div className="col-md-3 col-sm-3 hidden-xs chart">
                <Breakdown total={status.allRequests} data={status.requests} />
              </div>
              <div className="col-md-9 col-sm-9">
                <StatusList data={this.getRequestsData(status)} />
              </div>
            </div>
          </div>
          <div className="col-sm-12 col-md-6">
            <h2>Tasks</h2>
              <div className="row">
                <div className="col-md-3 col-sm-3 hidden-xs chart">
                  <Breakdown total={status.totalTasks} data={status.tasks} />
                </div>
                <div className="col-md-9 col-sm-9">
                  <StatusList data={this.getTasksData(status)} />
                  {this.renderTaskLag(status)}
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
                      {status.activeRacks} Active Racks
                    </Link>
                  ),
                  id: 'activeracks',
                  value: status.activeRacks
                },
                {
                  component: (className) => (
                    <Link to="racks/decommission" className={className}>
                      {status.decomissioningRacks} Decommissioning Racks
                    </Link>
                  ),
                  id: 'decomracks',
                  value: status.decomissioningRacks
                },
                {
                  component: (className) => (
                    <Link to="racks/inactive" className={className}>
                      {status.deadRacks} Inactive Racks
                    </Link>
                  ),
                  id: 'inactiveracks',
                  value: status.deadRacks
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
                      {status.activeSlaves} Active Slaves
                    </Link>
                  ),
                  value: status.activeSlaves,
                  id: 'activeslaves'
                },
                {
                  component: (className) => (
                    <Link to="slaves/decommission" className={className}>
                      {status.decomissioningSlaves} Decommissioning Slaves
                    </Link>
                  ),
                  value: status.decomissioningSlaves,
                  id: 'decomslaves'
                },
                {
                  component: (className) => (
                    <Link to="slaves/inactive" className={className}>
                      {status.deadSlaves} Inactive Slaves
                    </Link>
                  ),
                  className: status.deadSlaves > 0 ? 'color-warning' : '',
                  value: status.deadSlaves,
                  id: 'deadslaves'
                },
                status.unknownSlaves ? {
                  component: (className) => (
                    <Link to="slaves/inactive" className={className}>
                      {status.unknownSlaves} Unknown Slaves
                    </Link>
                  ),
                  className: 'color-warning',
                  value: status.unknownSlaves,
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
                    <span className={classNames(className, status.numDeploys < 2 && 'text-muted')}>
                      <strong>{status.numDeploys}</strong> Active Deploys
                    </span>
                  ),
                  value: status.numDeploys,
                  id: 'numdeploys'
                },
                status.oldestDeploy !== 0 ? {
                  component: (className) => (
                    <span className={className}>
                      <strong>{Utils.duration(status.oldestDeploy)}</strong> since last deploy
                    </span>
                  )
                } : null
              ]}
            />
          </div>
        </div>
        <div className="row">
          <div className="col-sm-12">
            <HostStates hosts={status.hostStates} />
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
