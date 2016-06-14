import React from 'react';
import { connect } from 'react-redux';

import { DeployState } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';

class DeployDetail extends React.Component {

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

  render() {
    console.log(this.props.deploy);
    let d = this.props.deploy;
    return (
      <div>
        {this.renderHeader(d)}
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
