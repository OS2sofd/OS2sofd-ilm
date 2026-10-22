/**
 * Validates input fields with configurable validation rules.
 * @param {Array<Object>} validations - Array of validation objects, each containing:
 *   - {*} value - The value to validate (or array of values for 'oneOf' type)
 *   - {string} message - The warning message to display if validation fails
 *   - {string} [type='notEmpty'] - The type of validation: 'notEmpty', 'notZero', 'greaterThan', 'oneOf', 'custom'
 *   - {*} [compareValue] - For 'greaterThan' type, the value to compare against
 *   - {Function} [condition] - For 'custom' type, a function that returns true if validation passes
 * @returns {boolean} - Returns true if all validations pass, false otherwise.
 *
 * @example
 * const isValid = verifyInputs([
 *     { value: business, message: "Du mangler at vælge en virksomhed", type: 'notZero' },
 *     { value: name, message: "Du mangler at indtaste et navn" },
 *     { value: startDate, message: "Stop dato må ikke være før start dato", type: 'greaterThan', compareValue: stopDate },
 *     { value: [cpr, uuid], message: "Du mangler at indtaste en CPR eller en UUID", type: 'oneOf' }
 * ]);
 */
export default function verifyInputs(validations) {
    for (let validation of validations) {
        const { value, message, type = 'notEmpty', compareValue, condition } = validation;
        let isValid = true;

        switch (type) {
            case 'notEmpty':
                isValid = value !== null && value !== undefined && value !== '';
                break;
            case 'notZero':
                isValid = value !== "0" && value !== 0;
                break;
            case 'greaterThan':
                isValid = compareValue ? !(value > compareValue) : true;
                break;
            case 'oneOf':
                // value should be an array, check if at least one is truthy
                isValid = Array.isArray(value) && value.some(v => v !== null && v !== undefined && v !== '');
                break;
            case 'oneOfNotFalse':
                isValid = Array.isArray(value) && value.some(v => v !== null && v !== undefined && v !== false)
                break;
            case 'oneOfNotZero':
                isValid = Array.isArray(value) && value.some(v => v !== "0" && v !== 0);
                break;
            case 'custom':
                isValid = condition ? condition() : true;
                break;
            default:
                isValid = value !== null && value !== undefined && value !== '';
        }

        if (!isValid) {
            toastr.warning(message);
            return false;
        }
    }
    return true;
}
