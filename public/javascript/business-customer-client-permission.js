function showHideClientPermissionFunc() {
  var selectedDiv = $('#hidden-identifiers');
  var submitButton = $('#submit');
  var continueButton = $('#continue');
  var permissionFalse = $("#client-permission-false-hidden");
  var permissionTrue = $("#client-permission-true-hidden");

  submitButton.hide();
  continueButton.show();
  permissionFalse.hide();
  permissionTrue.hide();

  $('input[type=radio][name=permission]').change(function(){
    if(this.value == 'true') {
      submitButton.hide();
      continueButton.show();
      permissionFalse.hide();
      permissionTrue.show();
    } else {
      submitButton.show();
      continueButton.hide();
      permissionFalse.show();
      permissionTrue.hide();
    }
  });

}

$(document).ready(function() {
  showHideClientPermissionFunc();
});
