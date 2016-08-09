import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import { getClickComponent } from '../modal/ModalWrapper';
import { FetchDeployForRequest } from '../../../actions/api/history';

import RedeployModal from './RedeployModal';
import Utils from '../../../utils';

const redeployTooltip = (
  <ToolTip id="redeploy">
    Redeploy
  </ToolTip>
);

class RedeployButton extends Component {

  static propTypes = {
    fetchDeploy: PropTypes.func.isRequired,
    requestId: PropTypes.string.isRequired,
    deployId: PropTypes.string.isRequired,
    deploy: PropTypes.object,
    doAfterRedeploy: PropTypes.func,
    children: PropTypes.node
  };

  static defaultProps = {
    children: (
      <OverlayTrigger placement="top" id="view-redeploy-overlay" overlay={redeployTooltip}>
        <a title="Redeploy">
          <Glyphicon glyph="repeat" />
        </a>
      </OverlayTrigger>
    )
  }

  render() {
    return (
      <span>
        {getClickComponent(this, this.props.fetchDeploy)}
        <RedeployModal ref="modal" {...this.props} />
      </span>
    );
  }
}

const mapStateToProps = (state) => ({
  state,
  deploy: Utils.maybe(state.api.deploy, ['data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchDeploy: () => dispatch(FetchDeployForRequest.trigger(ownProps.requestId, ownProps.deployId)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RedeployButton);
