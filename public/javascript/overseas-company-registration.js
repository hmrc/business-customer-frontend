function clearInputValues() {

    $('input[type=radio][name=hasBusinessUniqueId]').change(function(){
        if(this.value == 'true') {
          // Do nothing
        } else {
            $('#businessUniqueId').val('');
            $('#issuingCountry-select').prop('selectedIndex',0);
            $('#issuingCountry').val('');
            $('#issuingInstitution').val('');
        }
    });

}