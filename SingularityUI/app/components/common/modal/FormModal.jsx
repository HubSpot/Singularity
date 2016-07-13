import React from 'react';
import classNames from 'classnames';

import { Modal, Button } from 'react-bootstrap';
import TagsInput from 'react-tagsinput';
import Duration from '../formItems/Duration';
import Select from 'react-select';

export default class FormModal extends React.Component {
  constructor(props) {
    super(props);
    const formState = {};
    _.each(props.formElements, (formElement) => {
      formState[formElement.name] = formElement.defaultValue && formElement.defaultValue.toString();
    });

    this.state = {
      visible: false,
      formState,
      errors: {}
    };

    this.hide = this.hide.bind(this);
    this.show = this.show.bind(this);
    this.confirm = this.confirm.bind(this);
  }

  static FormItem = (props) => {
    if ((props.element.dependsOn && props.formState[props.element.dependsOn]) || !props.element.dependsOn) {
      return (
        <div className={classNames(props.className, {'childItem': props.formState[props.element.dependsOn]})}>
          {props.children}
        </div>
      );
    }
    return null;
  };

  static INPUT_TYPES = {
    BOOLEAN: 'BOOLEAN',
    STRING: 'STRING',
    RADIO: 'RADIO',
    TAGS: 'TAGS',
    NUMBER: 'NUMBER',
    DURATION: 'DURATION',
    SELECT: 'SELECT'
  };

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
    this.setState({ formState });
  }

  validateForm() {
    // Check required values
    const errors = {};
    _.each(this.props.formElements, (formElement) => {
      if (!this.state.formState[formElement.name] && formElement.isRequired) {
        errors[formElement.name] = 'This field is required';
      } else if (formElement.validateField) {
        const error = formElement.validateField(this.state.formState[formElement.name]);
        if (error) {
          errors[formElement.name] = error;
        }
      }
    });
    this.setState({ errors });

    // Returns true if form is valid
    return _.isEmpty(errors);
  }

  parseFormState(state) {
    const parsed = {};
    _.mapObject(state, (val, key) => {
      const element = _.find(this.props.formElements, (formElement) => formElement.name === key);
      switch (element.type) {
        case FormModal.INPUT_TYPES.BOOLEAN:
          parsed[key] = Boolean(val);
          break;
        case FormModal.INPUT_TYPES.NUMBER:
          parsed[key] = Number.parseFloat(val);
          break;
        default:
          parsed[key] = val;
      }
    });
    return parsed;
  }

  confirm() {
    if (this.validateForm()) {
      this.props.onConfirm(this.parseFormState(this.state.formState));
      const formState = {};
      _.each(this.props.formElements, (formElement) => {
        formState[formElement.name] = formElement.defaultValue;
      });
      this.setState({
        visible: false,
        errors: {},
        formState
      });
    }
  }

  renderForm() {
    const inputs = this.props.formElements.map((formElement) => {
      const error = this.state.errors[formElement.name];
      const errorBlock = error && <span className="help-block">{error}</span>;
      const help = formElement.help && <span className="help-block">{formElement.help}</span>;

      const radioButtons = () => _.map(formElement.values, (value, key) => {
        return (
          <div key={key} className="radio">
            <label>
              <input type="radio"
                name={formElement.name}
                checked={value.value.toString() === this.state.formState[formElement.name]}
                value={value.value}
                onChange={(event) => this.handleFormChange(formElement.name, event.target.value)}
              />
            {value.label}
            </label>
          </div>
        );
      });

      switch (formElement.type) {

        case FormModal.INPUT_TYPES.BOOLEAN:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>
                  <input
                    type="checkbox"
                    name={formElement.name}
                    checked={this.state.formState[formElement.name] || false}
                    onChange={(event) => this.handleFormChange(formElement.name, event.target.checked)}
                  /> {formElement.label}
                </label>
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.STRING:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
                <input type="text"
                  name={formElement.name}
                  className="form-control input-large"
                  value={this.state.formState[formElement.name] || ''}
                  onChange={(event) => this.handleFormChange(formElement.name, event.target.value)}
                />
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.RADIO:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <strong>{formElement.label}</strong>
              {radioButtons()}
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.TAGS:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <label style={{display: 'block', width: '100%'}}>
                {formElement.label}
                <TagsInput
                  value={this.state.formState[formElement.name] || []}
                  onChange={(tags) => this.handleFormChange(formElement.name, tags)}
                  addOnBlur={true}
                  addOnPaste={true}
                  inputProps={{className: 'form-control input-large', placeholder: ''}}
                />
              </label>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.NUMBER:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
                <input type="number"
                  name={formElement.name}
                  min={formElement.min}
                  max={formElement.max}
                  step={formElement.step}
                  className="form-control input-large"
                  value={this.state.formState[formElement.name] || ''}
                  onChange={(event) => this.handleFormChange(formElement.name, event.target.value)}
                />
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.DURATION:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
                <Duration type="text"
                  value={this.state.formState[formElement.name] || 0}
                  onChange={(value) => this.handleFormChange(formElement.name, value)}
                />
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.SELECT:
          if (formElement.options.length < 6 && !formElement.useSelectDespiteFewOptions) {
            const buttons = formElement.options.map((option, key) =>
              <button
                key={key}
                value={option.value}
                className={classNames('btn', 'btn-default', {active: this.state.formState[formElement.name] === option.value})}
                onClick={(event) => {event.preventDefault(); return this.handleFormChange(formElement.name, option.value);}}
              >
                {option.label}
              </button>
            );
            return (
              <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
                <div className={classNames('form-group', {'has-error': !!error})}>
                  <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
                  <div id={formElement.name} className="input-group-btn btn-group">{buttons}</div>
                  {errorBlock}
                  {help}
                </div>
              </FormModal.FormItem>
            );
          }
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
                <Select
                  options={formElement.options}
                  clearable={formElement.clearable}
                  value={this.state.formState[formElement.name] || ''}
                  id={formElement.name}
                  onChange={(value) => this.handleFormChange(formElement.name, value.value)}
                />
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        default:
          return undefined;
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
      <Modal show={this.state.visible} onHide={this.hide}>
        <Modal.Body>
          {this.props.name && <h3>{this.props.name}</h3>}
          {this.props.name && <hr />}
          {this.props.children}
          {this.props.children && !!this.props.formElements.length && <hr />}
          {this.renderForm()}
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle="default" onClick={this.hide}>Cancel</Button>
          <Button bsStyle={this.props.buttonStyle} onClick={this.confirm}>{this.props.action}</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

FormModal.propTypes = {
  action: React.PropTypes.node.isRequired,
  onConfirm: React.PropTypes.func.isRequired,
  buttonStyle: React.PropTypes.string,
  name: React.PropTypes.string,
  children: React.PropTypes.node,
  formElements: React.PropTypes.arrayOf(React.PropTypes.shape({
    options: React.PropTypes.arrayOf(React.PropTypes.shape({
      value: React.PropTypes.string.isRequired,
      label: React.PropTypes.string.isRequired
    })),
    useSelectDespiteFewOptions: React.PropTypes.bool,
    clearable: React.PropTypes.bool,
    name: React.PropTypes.string.isRequired,
    type: React.PropTypes.oneOf(_.keys(FormModal.INPUT_TYPES)).isRequired,
    label: React.PropTypes.string,
    isRequired: React.PropTypes.bool,
    values: React.PropTypes.array,
    defaultValue: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.bool, React.PropTypes.number]),
    validateField: React.PropTypes.func, // String -> String, return field validation error or falsey value if valid
    dependsOn: React.PropTypes.string // Only show this item if the other item (referenced by name) has a truthy value
  })).isRequired
};
