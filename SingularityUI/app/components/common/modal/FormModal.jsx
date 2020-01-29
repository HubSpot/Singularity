import React from 'react';
import classNames from 'classnames';

import { Modal, Button, Popover, OverlayTrigger, Tooltip, DropdownButton, MenuItem } from 'react-bootstrap';
import TagsInput from 'react-tagsinput';
import MultiInput from '../formItems/MultiInput';
import Select from 'react-select';
import Utils from '../../../utils';
import juration from 'juration';

const TAGS_CHARACTER_LIMIT = 75;

function getDefaultFormState(props) {
  const formState = {};
  props.formElements.forEach((formElement) => {
    const { defaultValue } = formElement;
    if (defaultValue) {
      if (Array.isArray(defaultValue)) {
        formState[formElement.name] = defaultValue;
      } else {
        formState[formElement.name] = formElement.defaultValue.toString();
      }
    }
  });
  return formState;
}

export default class FormModal extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      visible: false,
      formState: getDefaultFormState(props),
      errors: {}
    };

    _.bindAll(this, 'hide', 'show', 'confirm');
  }

  componentWillReceiveProps(newProps) {
    if (_.isEqual(this.state.formState, getDefaultFormState(this.props))) {
      this.setState({formState: getDefaultFormState(newProps)});
    }
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
    MULTIINPUT: 'MULTIINPUT',
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
    const formState = Utils.deepClone(this.state.formState);
    formState[name] = value;
    this.setState({ formState });
  }

  isValidForm() {
    // Check required values
    const errors = {};
    this.props.formElements.forEach((formElement) => {
      if (!this.state.formState[formElement.name] && formElement.isRequired) {
        errors[formElement.name] = 'This field is required';
      } else if (formElement.validateField) {
        const error = formElement.validateField(this.state.formState[formElement.name]);
        if (error) {
          errors[formElement.name] = error;
        }
      }
      if (formElement.type === FormModal.INPUT_TYPES.DURATION && this.state.formState[formElement.name]) {
        try {
          juration.parse(this.state.formState[formElement.name]);
        } catch (exception) {
          errors[formElement.name] = 'Invalid duraton specified.';
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
      if (element !== undefined) {
        switch (element.type) {
          case FormModal.INPUT_TYPES.BOOLEAN:
            parsed[key] = Boolean(val);
            break;
          case FormModal.INPUT_TYPES.NUMBER:
            parsed[key] = Number.parseFloat(val);
            break;
          case FormModal.INPUT_TYPES.DURATION:
            if (val) {
              parsed[key] = juration.parse(val) * 1000;
            }
            break;
          default:
            parsed[key] = val;
        }
      }
    });
    return parsed;
  }

  confirm(event) {
    if (event) {
      event.preventDefault();
    }
    if (this.isValidForm()) {
      let formState = this.parseFormState(this.state.formState);
      this.props.onConfirm(formState);
      if (!this.props.keepCurrentFormState) {
        formState = {};
        this.props.formElements.forEach((formElement) => {
          formState[formElement.name] = formElement.defaultValue;
        });
      }
      this.setState({
        visible: false,
        errors: {},
        formState
      });
    }
  }

  renderTag(props) {
    const {tag, key, onRemove, ...other} = props;
    let tagDisplay;
    if (tag.length > TAGS_CHARACTER_LIMIT) {
      const tooltip = (
        <Popover id="full-tag" className="tag-popover">{tag}</Popover>
      );
      tagDisplay = (
        <OverlayTrigger
          trigger="hover"
          placement="left"
          overlay={tooltip}
        >
          <span>{`${tag.substr(0, TAGS_CHARACTER_LIMIT)}...`}</span>
        </OverlayTrigger>
      );
    } else {
      tagDisplay = tag;
    }
    return (
      <span key={key} {...other}>
        {tagDisplay}
        <a onClick={() => onRemove(key)} />
      </span>
    );
  }

  renderTagsInput(props) {
    const inputProps = _.omit(props, 'className', 'placeholder', 'addTag');
    return (
      <div className="input-group">
        <input type="text" ref="argValue" className="form-control" placeholder="" {...inputProps} />
        <span className="input-group-btn">
          <Button bsStyle="success" onClick={() => this.refs.tagsInput.accept()}>+</Button>
        </span>
      </div>
    );
  }

  renderFormattedOptions(optionValue) {
    if (_.isArray(optionValue)) {
      let optionLines = optionValue.map((value, index) =>
        (<li key={index}>{value}</li>)
      );
      return (
        <ul>{optionLines}</ul>
      );
    } else {
      return optionValue;
    }
  }

  renderTooltipOptions(optionValue) {
    if (_.isArray(optionValue)) {
      return (
        <Tooltip id="options">
          { optionValue.map((option, index) => <span key={index}>{option}<br /></span>) }
        </Tooltip>
      );
    } else {
      return <Tooltip>{ optionValue }</Tooltip>;
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

      const selectOptions = () => {
        if (formElement.valueOptions && formElement.valueOptions.length > 0) {
          const menuItems = [];
          _.each(formElement.valueOptions, (optionValue, index) => {
            if (index < 5) {
              if (index !== 0) {
                menuItems.push(<MenuItem divider={true} />);
              }
              menuItems.push(
                <OverlayTrigger
                  placement="top"
                  overlay={this.renderTooltipOptions(optionValue)}
                  >
                  <MenuItem
                    eventKey={index}
                    onSelect={() => this.handleFormChange(formElement.name, optionValue)}
                    className="select-options"
                  >
                    {this.renderFormattedOptions(optionValue)}
                  </MenuItem>
                </OverlayTrigger>
              );
            }
          });

          return (
            <DropdownButton
              pullRight={true}
              bsStyle="info"
              bsSize="small"
              title="Previous Args"
              id={`${formElement.name}-input-dropdown-options`}
            >
              {menuItems}
            </DropdownButton>
          );
        }
        return null;
      };

      let extraHelp;

      if (formElement.type === FormModal.INPUT_TYPES.DURATION) {
        extraHelp = 'Accepts any English duration (Days, Hr, Min...)';
      }

      switch (formElement.type) {

        case FormModal.INPUT_TYPES.BOOLEAN:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <div className={classNames('form-group', {'has-error': !!error})}>
                <label className="control-label" htmlFor={formElement.name}>
                  <input
                    type="checkbox"
                    id={formElement.name}
                    name={formElement.name}
                    checked={this.state.formState[formElement.name] || false}
                    onChange={(event) => this.handleFormChange(formElement.name, event.target.checked)}
                  />
                  {' '}{formElement.label}
                </label>
                {errorBlock}
                {help}
              </div>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.DURATION:
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
                {extraHelp}
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
                  ref="tagsInput"
                  value={this.state.formState[formElement.name] || []}
                  onChange={(tags) => this.handleFormChange(formElement.name, tags)}
                  addOnBlur={true}
                  renderInput={(props) => this.renderTagsInput(props)}
                  renderTag={this.renderTag}
                />
              </label>
            </FormModal.FormItem>
          );

        case FormModal.INPUT_TYPES.MULTIINPUT:
          return (
            <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
              <label style={{display: 'block', width: '100%'}}>
                {formElement.label}
                <span className="pull-right">
                  {selectOptions()}
                </span>
                <MultiInput
                  id={`${formElement.name}-input`}
                  value={this.state.formState[formElement.name] || []}
                  onChange={(values) => this.handleFormChange(formElement.name, values)}
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

        case FormModal.INPUT_TYPES.URL:
          return (
          <FormModal.FormItem element={formElement} formState={this.state.formState} key={formElement.name}>
            <div className={classNames('form-group', {'has-error': !!error})}>
              <label className="control-label" htmlFor={formElement.name}>{formElement.label}</label>
              <input type="url"
                name={formElement.name}
                className="form-control input-large"
                value={this.state.formState[formElement.name] || ''}
                onChange={(event) => this.handleFormChange(formElement.name, event.target.value)}
              />
              {help}
            </div>
          </FormModal.FormItem>
        );

        default:
          return undefined;
      }
    });

    return (
      <form className="modal-form" onSubmit={(event) => this.confirm(event)}>
        {inputs}
      </form>
    );
  }

  render() {
    const cancel = !this.props.mustFill && <Button bsStyle="default" onClick={this.hide}>Cancel</Button>;

    return (
      <Modal show={this.state.visible} onHide={this.hide} backdrop={this.props.mustFill ? 'static' : true}>
        {this.props.name && <Modal.Header><h3>{this.props.name}</h3></Modal.Header>}
        <Modal.Body>
          {this.props.children}
          {this.props.children && !!this.props.formElements.length && <hr />}
          {this.renderForm()}
        </Modal.Body>
        <Modal.Footer>
          {cancel}
          <Button bsStyle={this.props.buttonStyle} onClick={this.confirm} disabled={this.props.disableSubmit}>{this.props.action}</Button>
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
  mustFill: React.PropTypes.bool,
  disableSubmit: React.PropTypes.bool,
  keepCurrentFormState: React.PropTypes.bool,
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
    defaultValue: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.bool, React.PropTypes.number, React.PropTypes.array]),
    validateField: React.PropTypes.func, // String -> String, return field validation error or falsey value if valid
    dependsOn: React.PropTypes.string // Only show this item if the other item (referenced by name) has a truthy value
  })).isRequired
};
