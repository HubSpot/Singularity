import React from 'react';
import HostStates from './HostStates';
import StatusList from './StatusList';
import Breakdown from './Breakdown';
import Link from '../common/atomicDisplayItems/Link';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import PlainText from '../common/atomicDisplayItems/PlainText';

export default class StatusPage extends React.Component {

  renderPercentage(number, total) {
    return number > 0 ? `(${Math.round(number/total * 100)}%)` : '';
  }

  getRequestsData() {
    return this.props.requests.map((r) => {
      return (
        {
          component: Link,
          beforeFill: r.type,
          prop: {
            text: `${r.count} ${r.label} ${this.renderPercentage(r.count, this.props.model.allRequests)}`,
            url: `${config.appRoot}${r.link}`
          }
        }
      );
    });
  }

  getTasksData() {
    return this.props.tasks.map((t) => {
      return (
        {
          component: Link,
          beforeFill: t.type,
          prop: {
            text: `${t.count} ${t.label} ${this.renderPercentage(t.count, this.props.totalTasks)}`,
            url: `${config.appRoot}${t.link}`
          }
        }
      );
    });
  }

  render() {
    console.log(this.props);
    let m = this.props.model;
    return (
      <div>
        <div className="row">
          <div className="col-sm-12 col-md-6">
            <h2>Requests</h2>
            <div className="row">
              <div className="col-md-3 col-sm-3 hidden-xs chart">
                <Breakdown total={m.allRequests} data={this.props.requests} />
              </div>
              <div className="col-md-9 col-sm-9">
                <StatusList data={this.getRequestsData()} />
              </div>
            </div>
          </div>
          <div className="col-sm-12 col-md-6">
            <h2>Tasks</h2>
              <div className="col-md-3 col-sm-3 hidden-xs chart">
                <Breakdown total={this.props.totalTasks} data={this.props.tasks} />
              </div>
              <div className="col-md-9 col-sm-9">
                <StatusList data={this.getTasksData()} />
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
                    url: `${config.appRoot}/racks/active`
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.decomissioningRacks} Decommissioning Racks`,
                    url: `${config.appRoot}/racks/decommission`
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.deadRacks} Inactive Racks`,
                    url: `${config.appRoot}/racks/inactive`
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
                    url: `${config.appRoot}/slaves/active`
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.decomissioningSlaves} Decommissioning Slaves`,
                    url: `${config.appRoot}/slaves/decommission`
                  }
                },
                {
                  component: Link,
                  prop: {
                    text: `${m.deadSlaves} Inactive Slaves`,
                    url: `${config.appRoot}/slaves/inactive`,
                    className: m.deadSlaves > 0 ? 'color-warning' : ''
                  }
                },
                m.unknownSlaves ? {
                  component: Link,
                  prop: {
                    text: `${m.unknownSlaves} Unknown Slaves`,
                    url: `${config.appRoot}/slaves/inactive`,
                    className: 'color-warning'
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
                    className: m.numDeploys < 2 ? 'text-muted' : ''
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

StatusPage.propTypes = {};
