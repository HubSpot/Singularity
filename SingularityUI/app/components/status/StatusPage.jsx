import React from 'react';
import HostStates from './HostStates';
import Breakdown from './Breakdown';
import Link from '../common/atomicDisplayItems/Link';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import PlainText from '../common/atomicDisplayItems/PlainText';

export default class StatusPage extends React.Component {

  render() {
    console.log(this.props);
    let m = this.props.model;
    return (
      <div>
        <div className="row">
          <div className="col-md-4 col-sm-12">
            <Breakdown
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
            <Breakdown
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
                !m.unknownSlaves ? {
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
            <Breakdown
              header="Deploys"
              data={[
                {
                  component: PlainText,
                  prop: {
                    text: `${m.numDeploys} Active Deploys`,
                    className: m.numDeploys < 2 ? 'text-muted' : ''
                  }
                },
                m.oldestDeploy == 0 ? {
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
