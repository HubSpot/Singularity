import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchPriorityFreeze, NewPriorityFreeze } from '../../actions/api/disasters';
import FormModal from '../common/modal/FormModal';

import Utils from '../../utils';

class NewPriorityFreezeModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    newPriorityFreeze: PropTypes.func.isRequired
  };

  show() {
    this.refs.newPriorityFreezeModal.show();
  }

  render() {
    return (
      <FormModal
        ref="newPriorityFreezeModal"
        name="New Priority Freeze"
        action="Create Priority Freeze"
        buttonStyle="warning"
        onConfirm={(data) => this.props.newPriorityFreeze(data.minimumPriorityLevel, data.killTasks, data.message)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.NUMBER,
            name: 'minimumPriorityLevel',
            label: 'Minimum Priority Level',
            isRequired: true,
            max: 1.0,
            min: 0
          },
          {
            type: FormModal.INPUT_TYPES.BOOLEAN,
            name: 'killTasks',
            label: 'Kill Tasks?'
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
  newPriorityFreeze: (minPriority, killTasks, message) => dispatch(NewPriorityFreeze.trigger(minPriority, killTasks, message)).then(() => {dispatch(FetchPriorityFreeze.trigger([404]));}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(NewPriorityFreezeModal);
