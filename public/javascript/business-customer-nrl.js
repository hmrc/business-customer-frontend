function showHideNrlFunc() {
  var hiddenUtr = $('#hidden-uniqueTaxRef-true');
  var hiddenShadeBox = $('#shade-box');
  var submitButton = $('#submit');
  var continueButton = $('#continue');

  hiddenUtr.hide();
  hiddenShadeBox.hide();
  submitButton.hide();

  $('input[type=radio][name=paysSA]').change(function(){
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
  showHideNrlFunc();
});
