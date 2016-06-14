import React from 'react';
import { connect } from 'react-redux';
import Clipboard from 'clipboard';
import Utils from '../../utils';

import { DeployState, InfoBox } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';

class DeployDetail extends React.Component {

  componentDidMount() {
    new Clipboard('.info-copyable');
  }

  renderHeader(d) {
    let message;
    if (d.deployResult.message) {
      message = (
        <div className="row">
            <div className="col-md-12">
                <div className="well text-muted">
                    {d.deployResult.message}
                </div>
            </div>
        </div>
      );
    }
    let failures;
    if (d.deployResult.deployFailures) {
      let fails = [];
      let k = 0;
      for (let f of d.deployResult.deployFailures) {
        fails.push(f.taskId ?
          <a key={k} href={`${config.appRoot}/task/${f.taskId.id}`} className="list-group-item">
            <strong>{f.taskId.id}</strong>: {f.reason} (Instance {f.taskId.instanceNo}): {f.message}
          </a>
          :
          <li key={k} className="list-group-item">{f.reason}: {f.message}</li>
        )
        k++;
      }
      if (fails.length) {
        failures = (
          <div className="row">
              <div className="col-md-12">
                  <div className="panel panel-danger">
                      <div className="panel-heading text-muted">Deploy had {fails.length} failure{fails.length > 1 ? 's' : ''}:</div>
                      <div className="panel-body">
                        {fails}
                      </div>
                  </div>
              </div>
          </div>
        );
      }
    }
    return (
      <header className='detail-header'>
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={[{
                  label: "Request",
                  text: d.deploy.requestId,
                  link: `${config.appRoot}/request/${d.deploy.requestId}`
                }]}
            />
          </div>
        </div>
        <div className="row">
          <div className="col-md-8">
            <h3>
              <span>{d.deploy.id}</span>
              <DeployState state={d.deployResult.deployState} />
            </h3>
          </div>
          <div className="col-md-4 button-container">
            <JSONButton object={d} />
          </div>
        </div>
        {failures || message}
      </header>
    );
  }

  renderInfo(d) {
    let stats = [];

    if (d.deployMarker.timestamp) {
      stats.push(<InfoBox key='initiated' copyableClassName="info-copyable" name="Initiated" value={Utils.timeStampFromNow(d.deployMarker.timestamp)} />);
    }
    if (d.deployResult.timestamp) {
      stats.push(<InfoBox key='completed' copyableClassName="info-copyable" name="Completed" value={Utils.timeStampFromNow(d.deployResult.timestamp)} />);
    }
    if (d.deploy.executorData && d.deploy.executorData.cmd) {
      stats.push(<InfoBox key='cmd' copyableClassName="info-copyable" name="Command" value={d.deploy.executorData.cmd} />);
    }
    if (d.deploy.resources.cpus) {
      let value = `CPUs: ${d.deploy.resources.cpus} | Memory (Mb): ${d.deploy.resources.memoryMb} | Ports: ${d.deploy.resources.numPorts}`;
      stats.push(<InfoBox key='cpus' copyableClassName="info-copyable" name="Resources" value={value} />);
    }
    if (d.deploy.executorData && d.deploy.executorData.extraCmdLineArgs) {
      stats.push(<InfoBox key='args' copyableClassName="info-copyable" name="Extra Command Line Arguments" value={d.deploy.executorData.extraCmdLineArgsd} />);
    }

    for (let s in d.deployStatistics) {
      if (typeof d.deployStatistics[s] !== 'object') {
        let value = typeof d.deployStatistics[s] === 'string' ? Utils.humanizeText(d.deployStatistics[s]) : d.deployStatistics[s];
        stats.push(
          <InfoBox copyableClassName="info-copyable" key={s} name={Utils.humanizeCamelcase(s)} value={value} />
        );
      }
    }
    return (
      <div>
        <div className="page-header">
            <h2>Info</h2>
        </div>
        <div className="row">
          <ul className="list-unstyled horizontal-description-list">
            {stats}
          </ul>
        </div>
      </div>
    );
  }

  render() {
    console.log(this.props.deploy);
    let d = this.props.deploy;
    return (
      <div>
        {this.renderHeader(d)}
        {this.renderInfo(d)}
      </div>
    );
  }
}

function mapStateToProps(state) {
    return {
        deploy: state.api.deploy.data
    };
}

export default connect(mapStateToProps)(DeployDetail);
