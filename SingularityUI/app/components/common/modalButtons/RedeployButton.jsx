import React, { PropTypes } from 'react';
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

const RedeployButton = (props) => {
  const clickComponentData = {props}; // Tricks getClickComponent() into doing the right thing despite this being functional. Muahahaha
  return (
    <span>
      {getClickComponent(clickComponentData, props.fetchDeploy)}
      <RedeployModal ref={(modal) => {if (modal) clickComponentData.refs = {modal};}} {...props} />
    </span>
  );
};

RedeployButton.propTypes = {
  fetchDeploy: PropTypes.func.isRequired,
  requestId: PropTypes.string.isRequired,
  deployId: PropTypes.string.isRequired,
  deploy: PropTypes.object,
  doAfterRedeploy: PropTypes.func,
  children: PropTypes.node
};

RedeployButton.defaultProps = {
  children: (
    <OverlayTrigger placement="top" id="view-redeploy-overlay" overlay={redeployTooltip}>
      <a title="Redeploy">
        <Glyphicon glyph="repeat" />
      </a>
    </OverlayTrigger>
  )
};

const mapStateToProps = (state) => ({
  state,
  deploy: Utils.maybe(state.api.deploy, ['data'])
});

const mapDispatchToProps = (dispatch, ownProps) => ({
  fetchDeploy: () => dispatch(FetchDeployForRequest.trigger(ownProps.requestId, ownProps.deployId)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RedeployButton);
