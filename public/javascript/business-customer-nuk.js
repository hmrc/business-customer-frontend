function showHideNukUtrFunc() {
  var selectedDiv = $('#hidden-identifiers');
  var hiddenUtr = $('#hidden-uniqueTaxRef-true');
  var hiddenShadeBox = $('#shade-box');
  var submitButton = $('#submit');
  var continueButton = $('#continue');

  hiddenUtr.hide();
  hiddenShadeBox.hide();
  submitButton.hide();

  if($('#nuk-utr-true').is(':checked')) {
    selectedDiv.show();
    submitButton.hide();
  }

  if($('#nUkUtr-true').is(':checked')) {
    selectedDiv.show();
  }

  $('input[type=radio][name=nUkUtr]').change(function(){
    if(this.value == 'true') {
      hiddenUtr.show();
      hiddenShadeBox.show();
      submitButton.show();
      continueButton.hide();
    } else {
      hiddenUtr.hide();
      hiddenShadeBox.hide();
      submitButton.hide();
      continueButton.show();

    }
  });

}

$(document).ready(function() {
  showHideNukUtrFunc();
});
