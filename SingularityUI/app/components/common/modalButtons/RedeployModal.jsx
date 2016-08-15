import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import { connect } from 'react-redux';
import { Alert } from 'react-bootstrap';
import JSONTree from 'react-json-tree';
import Messenger from 'messenger';
import { JSONTreeTheme } from '../../../thirdPartyConfigurations';

import { SaveDeploy } from '../../../actions/api/deploys';

import FormModal from '../modal/FormModal';
import Utils from '../../../utils';

class RedeployModal extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    saveDeploy: PropTypes.func,
    doAfterRedeploy: PropTypes.func,
    deploy: PropTypes.object
  };

  constructor(props) {
    super(props);
    this.state = {};
  }

  showSuccessMessage() {
    Messenger().success({
      message: `Success! Redeployed ${Utils.maybe(this.props.deploy, ['deploy', 'id'])}. New id: ${this.state.newDeployId}`,
      hideAfter: 10
    });
  }

  show() {
    this.refs.redeployModal.show();
  }

  confirmRedeploy(data) {
    this.props.saveDeploy(data.deployId).then((response) => {
      if (response.statusCode === 200) {
        this.setState({newDeployId: data.deployId});
        this.showSuccessMessage();
      } else {
        this.setState({error: response.error, errorCode: response.statusCode, newDeployId: data.deployId});
        this.show();
      }
      if (this.props.doAfterRedeploy) {
        this.props.doAfterRedeploy(response);
      }
    });
  }

  render() {
    const deployToShow = _.omit(Utils.maybe(this.props.deploy, ['deploy']), 'id');
    const deployId = Utils.maybe(this.props.deploy, ['deployMarker', 'deployId']);
    return (
      <FormModal
        name="Redeploy"
        ref="redeployModal"
        action="Redeploy"
        onConfirm={(data) => this.confirmRedeploy(data)}
        buttonStyle="primary"
        disableSubmit={_.isEmpty(deployToShow)}
        formElements={_.isEmpty(deployToShow) ? [] : [
          {
            name: 'deployId',
            type: FormModal.INPUT_TYPES.STRING,
            label: 'New Deploy Id (Must be unique)',
            defaultValue: deployId,
            isRequired: true,
            validateField: (newDeployId) => newDeployId === deployId && 'New deploy id must not be the same as the old deploy id'
          }
        ]}>
        {this.state.error && (
          <Alert bsStyle="danger">
            <p>Failed to redeploy {deployId}. The server responded with a <code>HTTP {this.state.errorCode}</code> and said:</p>
            <p><code>{this.state.error}</code></p>
          </Alert>
        )}
        {_.isEmpty(deployToShow) ? (
          <Alert bsStyle="danger">
            <p>We could not find old deploy info, and so are unable to redeploy this deploy.</p>
          </Alert>
        ) : (
          <div>
            <p>Are you sure you want to redeploy this deploy?</p>
            <pre>{deployId}</pre>
            <p>This will create a new deploy with the same attributes as the old deploy:</p>
            <JSONTree
              data={{deploy: deployToShow}}
              hideRoot={true}
              theme={JSONTreeTheme}
            />
          </div>
        )}
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  saveDeploy: (deployId) => dispatch(SaveDeploy.trigger({deploy: _.extend({}, ownProps.deploy.deploy, {id: deployId})})),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(RedeployModal);
