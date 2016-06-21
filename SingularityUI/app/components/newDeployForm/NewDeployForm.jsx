import React from 'react';
import { connect } from 'react-redux';
import FormField from '../common/formItems/FormField';
import DropDown from '../common/formItems/DropDown';
import CheckBox from '../common/formItems/CheckBox';
import { modifyField, clearForm } from '../../actions/form';
import {SaveAction} from '../../actions/api/request';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Utils from '../../utils';
import classNames from 'classnames';

const FORM_ID = 'newDeployForm';

class NewDeployForm extends React.Component {

  render() {
    return <h1>Hello world</h1>;
  }

}

function mapStateToProps(state) {
  return {
  request: state.api.request.request,
  form: state.form[FORM_ID],
  saveApiCall: state.api.saveDeploy
  }
}

function mapDispatchToProps(dispatch) {
  return {
  update(formId, fieldId, newValue) {
    dispatch(modifyField(formId, fieldId, newValue));
  },
  clearForm(formId) {
    dispatch(clearForm(formId));
  },
  save(requestBody) {
    dispatch(SaveAction.trigger(requestBody));
  }
  }
}

export default connect(mapStateToProps, mapDispatchToProps, undefined, {pure: false})(NewDeployForm);
