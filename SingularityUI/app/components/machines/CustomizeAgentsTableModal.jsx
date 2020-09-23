import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import FormModal from '../common/modal/FormModal';
import Utils from '../../utils';
import { UpdateAgentsTableSettings } from '../../actions/ui/agents';

class CustomizeAgentsTableModal extends Component {
  static propTypes = {
    columns: PropTypes.object.isRequired,
    paginated: PropTypes.bool.isRequired,
    availableAttributes: PropTypes.arrayOf(PropTypes.string).isRequired,
    availableResources: PropTypes.arrayOf(PropTypes.string).isRequired,
    updateAgentTableSettings: PropTypes.func.isRequired
  };

  show() {
    this.refs.customizeAgentsTableModal.show();
  }

  render() {
    const formElements = [];
    for (var field in Utils.DEFAULT_AGENTS_COLUMNS) {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Default Field: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    }
    _.each(this.props.availableResources, (field) => {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Resource: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    });
    _.each(this.props.availableAttributes, (field) => {
      formElements.push(
        {
          name: field,
          type: FormModal.INPUT_TYPES.BOOLEAN,
          label: `Attribute: ${field}`,
          defaultValue: field in this.props.columns ? this.props.columns[field] : false
        }
      )
    });
    formElements.push(
      {
        name: 'paginated',
        type: FormModal.INPUT_TYPES.BOOLEAN,
        label: 'paginated',
        defaultValue: this.props.paginated
      }
    );
    return (
      <FormModal
        ref="customizeAgentsTableModal"
        name="Customize Columns"
        action="Update"
        buttonStyle="default"
        onConfirm={(data) => this.props.updateAgentTableSettings(data)}
        keepCurrentFormState={true}
        formElements={formElements}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  updateAgentTableSettings: (data) => dispatch(UpdateAgentsTableSettings(data, data.paginated)),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(CustomizeAgentsTableModal);
