import React from 'react';
import classNames from 'classnames';

import { Modal, Button } from 'react-bootstrap';
import TagsInput from 'react-tagsinput'

export default class FormModal extends React.Component {

  static INPUT_TYPES = {
    BOOLEAN: 'BOOLEAN',
    STRING: 'STRING',
    RADIO: 'RADIO',
    TAGS: 'TAGS'
  }

  constructor(props) {
    super(props);
    const formState = {};
    _.each(props.formElements, (e) => {
      formState[e.name] = e.defaultValue;
    });
    this.state = {
      visible: false,
      formState: formState,
      errors: {}
    }
  }

  hide() {
    this.setState({
      visible: false
    });
  }

  show() {
    this.setState({
      visible: true
    });
  }

  handleFormChange(name, value) {
    const formState = this.state.formState;
    formState[name] = value;
    this.setState({
      formState: formState
    });
  }

  validateForm() {
    // Check required values
    const errors = {};
    _.each(this.props.formElements, (e) => {
      if (!this.state.formState[e.name] && e.isRequired) {
        errors[e.name] = 'This field is required'
      }
    });
    this.setState({
      errors: errors
    });

    // Returns true if form is valid
    return _.isEmpty(errors);
  }

  confirm() {
    if (this.validateForm()) {
      this.props.onConfirm(this.state.formState);
      const formState = {};
      _.each(this.props.formElements, (e) => {
        formState[e.name] = e.defaultValue;
      });
      this.setState({
        visible: false,
        formState: formState,
        errors: {}
      });
    }
  }

  renderForm() {
    const inputs = this.props.formElements.map((e) => {
      const error = this.state.errors[e.name];
      const help = error && <span className="help-block">{error}</span>;

      switch(e.type) {

        case FormModal.INPUT_TYPES.BOOLEAN:
          return (
            <div key={e.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" for={e.name}>
                  <input
                    type="checkbox"
                    name={e.name}
                    checked={this.state.formState[e.name]}
                    onChange={(event) => this.handleFormChange(e.name, event.target.checked)}
                  /> {e.label}
                </label>
                {help}
              </div>
            </div>
          );

        case FormModal.INPUT_TYPES.STRING:
          return (
            <div key={e.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" for={e.name}>{e.label}</label>
                <input type="text"
                  name={e.name}
                  className="form-control input-large"
                  value={this.state.formState[e.name] || ''}
                  onChange={(event) => this.handleFormChange(e.name, event.target.value)}
                />
                {help}
              </div>
            </div>
          );

        case FormModal.INPUT_TYPES.RADIO:
          const buttons = e.values.map((v, i) => {
            return (
              <div key={i} className="radio">
                <label>
                  <input type="radio"
                    name={e.name}
                    checked={v == this.state.formState[e.name]}
                    value={v}
                    onChange={(event) => this.handleFormChange(e.name, event.target.value)}
                  />
                {v}
                </label>
              </div>
            );
          });
          return (
            <div key={e.name}>
              <strong>{e.label}</strong>
              {buttons}
            </div>
          );

        case FormModal.INPUT_TYPES.TAGS:
          return (
            <div key={e.name}>
              <label style={{display: "block", width: "100%"}}>
                {e.label}
                <TagsInput
                  value={this.state.formState[e.name] || []}
                  onChange={(tags) => this.handleFormChange(e.name, tags)}
                  addOnBlur
                  addOnPaste
                  inputProps={{className: "form-control input-large", placeholder: ""}}
                />
              </label>
            </div>
          );
      }
    });

    return (
      <form className="modal-form">
        {inputs}
      </form>
    );
  }

  render() {
    return (
      <Modal show={this.state.visible} onHide={this.hide.bind(this)}>
        <Modal.Body>
          {this.props.children}
          {this.props.children && !!this.props.formElements.length && <hr/>}
          {this.renderForm()}
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle="default" onClick={this.hide.bind(this)}>Cancel</Button>
          <Button bsStyle={this.props.buttonStyle} onClick={this.confirm.bind(this)}>{this.props.action}</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

FormModal.propTypes = {
  action: React.PropTypes.node.isRequired,
  onConfirm: React.PropTypes.func.isRequired,
  buttonStyle: React.PropTypes.string,
  formElements: React.PropTypes.arrayOf(React.PropTypes.shape({
    name: React.PropTypes.string.isRequired,
    type: React.PropTypes.oneOf(_.keys(FormModal.INPUT_TYPES)).isRequired,
    label: React.PropTypes.string,
    required: React.PropTypes.bool,
    values: React.PropTypes.array,
    defaultValue: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.bool, React.PropTypes.number])
  })).isRequired
};
