import React from 'react';
import { connect } from 'react-redux';

import { DeployState } from '../common/statelessComponents';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';

class DeployDetail extends React.Component {

  renderHeader(d) {
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
