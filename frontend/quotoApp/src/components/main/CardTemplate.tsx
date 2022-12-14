import React from 'react';
import {
  Dimensions,
  StyleSheet,
  View,
  Text,
  Image,
  TouchableOpacity,
} from 'react-native';

import info from '../info';

import Icon from 'react-native-vector-icons/FontAwesome5';
import FontAwesome from 'react-native-vector-icons/FontAwesome';

interface Props {
  questName: string;
  questType: string;
  questImage: string;
  questDifficulty: number;
  handleRerollClick: any;
  isComplete: boolean;
}

const {width, height} = Dimensions.get('window');

const questTypes: {
  [key: string]: {
    typeName: string;
    iconName: string;
    questColorCode: string;
    stamp: any;
  };
} = info.questTypes;

const CardTemplate: React.FC<Props> = props => {
  const {
    questName,
    questType,
    questImage,
    handleRerollClick,
    isComplete,
    questDifficulty,
  } = props;

  const {typeName, iconName, questColorCode, stamp} = questTypes[questType];

  return (
    <View style={styles.card}>
      {isComplete ? null : (
        <TouchableOpacity style={styles.rerollIcon} onPress={handleRerollClick}>
          <Icon name="sync" color="#C7C7C7" size={20} />
        </TouchableOpacity>
      )}
      <View style={styles.labelContainer}>
        <View
          style={[
            styles.label,
            {
              backgroundColor: questColorCode,
            },
          ]}>
          <Text style={styles.labelContent}>
            <Icon name={iconName} size={15} />
            &nbsp;&nbsp; {typeName} 퀘스트
          </Text>
        </View>
        <View style={{paddingTop: 10}}>
          <Text>
            {[...Array(questDifficulty)].map((item, index) => (
              <FontAwesome
                key={index}
                name={isComplete ? 'star' : 'star-o'}
                color={questColorCode}
                size={19}
              />
            ))}
          </Text>
        </View>
      </View>
      <View style={styles.questContentContainer}>
        {questName.split('<br>').map((item, index) => (
          <Text
            key={index}
            style={[
              styles.questContent,
              {
                color: questColorCode,
              },
            ]}>
            {item}
          </Text>
        ))}
      </View>
      {isComplete ? (
        <Image resizeMode="contain" source={stamp} style={styles.stampImage} />
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    width: (width * 3) / 4,
    height: height / 2,
    backgroundColor: 'white',
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 20,
    elevation: 8,
  },
  rerollIcon: {
    position: 'absolute',
    top: 12,
    right: 15,
    padding: 5,
  },
  labelContainer: {
    flex: 1,
    alignItems: 'center',
  },
  label: {
    width: 150,
    height: 50,
    alignItems: 'center',
    borderBottomLeftRadius: 15,
    borderBottomRightRadius: 15,
  },
  labelContent: {
    color: 'white',
    textAlign: 'center',
    lineHeight: 50,
    fontFamily: 'Happiness-Sans-Bold',
    fontSize: 16,
  },
  questContentContainer: {
    flex: 9,
    justifyContent: 'center',
    padding: 20,
  },
  questContent: {
    textAlign: 'center',
    fontSize: 30,
    fontFamily: 'Happiness-Sans-Bold',
  },
  stampImage: {
    position: 'absolute',
    bottom: 0,
    right: 15,
    width: width * 0.3,
    height: width * 0.3,
  },
});

export default CardTemplate;
