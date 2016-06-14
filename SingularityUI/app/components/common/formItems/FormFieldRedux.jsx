import React from 'react';
import classNames from 'classnames';
import Utils from '../../../utils';
import { connect } from 'react-redux';
import { modifyField } from '../../../actions/form';

class FormField extends React.Component {

    update(event, props) {
        props.update (props.formId, props.fieldId, event.target.value);
    }

    render() {
        let props = this.props;
        return (
            <input 
                className={classNames('form-control', this.props.prop.customClass)}
                placeholder={this.props.prop.placeholder}
                type={this.props.prop.inputType}
                id={this.props.id}
                onChange={(event)=>this.update(event, props)}
                value={this.props.prop.value}
                disabled={this.props.prop.disabled}
                min={this.props.prop.min}
                max={this.props.prop.max}
                required={this.props.prop.required} />
        );
    }
};

function mapStateToProps(state) {
    return {
        state: state,
        racks: state.api.racks.data,
        request: state.api.request ? state.api.request.data : undefined
    }
}

function mapDispatchToProps(dispatch) {
    return {
        update: (formId, fieldId, newValue) => { dispatch(modifyField(formId, fieldId, newValue)); }
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(FormField);
