import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchDisabledActions, NewDisabledAction } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

const DISABLED_ACTION_TYPES = [
  'BOUNCE_REQUEST', 'SCALE_REQUEST', 'REMOVE_REQUEST', 'CREATE_REQUEST', 'UPDATE_REQUEST','KILL_TASK', 
  'BOUNCE_TASK', 'RUN_SHELL_COMMAND', 'ADD_METADATA', 'DEPLOY', 'CANCEL_DEPLOY', 'ADD_WEBHOOK', 'REMOVE_WEBHOOK',
  'TASK_RECONCILIATION', 'FREEZE_SLAVE', 'ACTIVATE_SLAVE', 'DECOMMISSION_SLAVE', 'VIEW_SLAVES','FREEZE_RACK',
  'ACTIVATE_RACK', 'DECOMMISSION_RACK', 'VIEW_RACKS'
];

import Utils from '../../utils';

class DeleteDisabledActionModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    newDisabledAction: PropTypes.func.isRequired
  };

  show() {
    this.refs.newDisabledActionModal.show();
  }

  render() {
    return (
      <FormModal
        ref="newDisabledActionModal"
        name="New Disabled Action"
        action="Create Disabled Action"
        buttonStyle="warning"
        onConfirm={(data) => this.props.newDisabledAction(data.type, data.message)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.SELECT,
            name: 'type',
            label: 'Type',
            isRequired: true,
            options: DISABLED_ACTION_TYPES.map((type) => ({
              label: Utils.humanizeText(type),
              value: type
            }))
          },
          {
            type: FormModal.INPUT_TYPES.STRING,
            name: 'message',
            label: 'Message',
            isRequired: false,
          }
        ]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  newDisabledAction: (type, message) => dispatch(NewDisabledAction.trigger(type, message)).then(() => {dispatch(FetchDisabledActions.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteDisabledActionModal);
