import React from 'react';
import { connect } from 'react-redux';

class DeployDetail extends React.Component {

  render() {
    console.log(this.props);
    let d = this.props.deploy;
    return (
      <h2>{d.deploy.id}</h2>
    );
  }
}

function mapStateToProps(state) {
    return {
        deploy: state.api.deploy.data
    };
}

export default connect(mapStateToProps)(DeployDetail);
